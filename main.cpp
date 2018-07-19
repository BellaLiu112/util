#include <stdio.h>
#include <sys/types.h>
#include <regex.h>
#include <memory.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <assert.h>

typedef struct line_s {
    char    *data; /* end with '\0' */
    short    size;
    line_s  *next;
    short    match;
    short    which;  /* which reg not match */
} line_t;

typedef struct reg_info_s {
    regex_t  *regs;
    int       reg_num;
    line_t   *lines;
    int       index;
} reg_info_t;


line_t *splite_lines(line_t *lines, int line_num, long group) {
    line_t   *groups;
    line_t   *group_ptr;
    long      counter = 0;
    line_t   *old = NULL;
    line_t   *next;
    long      n = line_num / group + 1;
    assert(lines);
    groups = (line_t *)malloc(sizeof(line_t) * group);
    if (!groups) return NULL;
    
    memset(groups, 0, sizeof(line_t) * group);
    group_ptr = groups;
    
    group_ptr->next = lines->next;
    next = lines->next;
    
    while (next) {
        counter++;
        if (!(counter % n)) {
            group_ptr++;
            group_ptr->next = next;
            if (old) old->next = NULL;
        }
        
        old = next;
        next = next->next;
    }
    return groups;
}

void free_lines(line_t *lines) {
    if (!lines) return;
    if (lines->next) {
        free_lines(lines->next);
    }
    free(lines);
}

line_t *parse_lines(const char* content, int *line_num) {
    line_t      *head;
    line_t      *next;
    const char  *s = content;
    const char  *ptr = content;
    long         line_len;
    int          num = 0;
    
    assert(content);
    head = (line_t *)malloc (sizeof(line_t));
    if (!head) return NULL;
    
    next = head;
    
    for (; *ptr ;) {
        if (*ptr == '\n') {
            line_len = ptr - s;
            if (*(ptr - 1) == '\r') {
                line_len--;
            }
            
            line_t *line = (line_t *)malloc(sizeof(line_t));
            line->data = (char*)malloc (line_len + 1);
            line->size = line_len;
            line->next = NULL;
            memcpy(line->data, s, line_len);
            *(line->data + line_len) = 0;
            next->next = line;
            next = line;
            s = ptr + 1;
            num++;
        }
        ptr++;
    }
    
    if ( s < ptr) {
        line_len = ptr - s;
        if (*(ptr - 1) == '\r') {
            line_len--;
        }
        line_t *line = (line_t *)malloc(sizeof(line_t));
        line->data = (char*)malloc (line_len + 1);
        line->size = line_len;
        line->next = NULL;
        memcpy(line->data, s, line_len);
        *(line->data + line_len) = 0;
        next->next = line;
        next = line;
        num++;
    }
    
    if (line_num) {
        *line_num = num;
    }
    
    return head;
}

char *read_file(const char* path, long *file_len) {
    FILE   *fp;
    char   *data;
    long    len;
    
    fp = fopen(path, "rb");
    if (!fp) return NULL;
    fseek(fp, 0 , SEEK_END);
    len = ftell(fp);
    if (file_len) *file_len = len;
    fseek(fp, 0, SEEK_SET);
    
    data = (char *)malloc (len + 1);
    if (data) {
        if (len != fread(data, 1, len, fp)){
            free(data);
            data = NULL;
        } else {
            *(data + len) = 0;
        }
    }
    fclose(fp);
    return data;
}

void* thread_callback(void  *arg) {
    reg_info_t *reg_info = (reg_info_t *)arg;
    line_t     *lines = reg_info->lines;
    regex_t    *regs = reg_info->regs;
    regex_t    *reg;
    int         reg_num = reg_info->reg_num;
    line_t     *line = lines->next;
    int         index, err;
    int         line_num = 0;
    
    printf ("thread %d start\n", reg_info->index);
    
    while (line) {
        line_num++;
        for (index = 0; index < reg_num; ++index) {
            reg = regs + index;
            err = regexec(reg, line->data, 0 , NULL, 0);
            
            if(err == 0){
                line->which = index;
                line->match = err;
                break;
            }
        }
        line = line->next;
    }
    
    printf ("thread %d end, process lines:%d\n",
            reg_info->index, line_num);
    
    return NULL;
}

regex_t *create_regex(line_t *reg_lines, int reg_num) {
    regex_t    *regs;
    regex_t    *reg_ptr;
    line_t     *line;
    char        errmsg[1024];
    int         rt = -1;
    
    if (!reg_lines || !reg_num) {
        return NULL;
    }
    
    regs = (regex_t *)malloc (sizeof(regex_t) * reg_num);
    if (!regs) {
        printf("alloc fail.\n");
        return NULL;
    }
    
    reg_ptr = regs;
    line = reg_lines->next;
    
    while (line) {
        rt = regcomp(reg_ptr, line->data, REG_EXTENDED) ;
        if(rt < 0){
            regerror(rt, reg_ptr, errmsg, sizeof(errmsg));
            printf("reg:%s err:%s\n", line->data, errmsg);
            break;
        }
        reg_ptr++;
        line = line->next;
    }
    
    if (rt != 0) {
        free(regs);
        regs = NULL;
    }
    
    return regs;
}

int process_regxs(line_t *groups, int group_num,
                  line_t *reg_lines, int reg_num) {
    
    pthread_t    *threads = NULL;
    reg_info_t   *reg_infos = NULL;
    int           index, rt = -1;
    
    threads = (pthread_t *)malloc(group_num * sizeof(pthread_t));
    if (!threads) goto end;
    memset(threads, 0, group_num * sizeof(pthread_t));
    
    reg_infos = (reg_info_t *)malloc (group_num * sizeof(reg_info_t));
    if (!reg_infos) goto end;
    memset(reg_infos, 0, group_num * sizeof(reg_info_t));
    
    for(index = 0; index < group_num; index++) {
        reg_info_t *reg_info = reg_infos + index;
        reg_info->index = index;
        reg_info->regs = create_regex(reg_lines, reg_num);
        if (!reg_info->regs) {
            rt = -1;
            break;
        }
        
        reg_info->reg_num = reg_num;
        reg_info->lines = groups + index;
        rt = pthread_create(threads + index ,NULL, thread_callback, reg_info);
        if (rt < 0) {
            printf("create thread fail %d", index);
            break;
        }
    }
    
    if (rt == 0) {
        for(index = 0; index < group_num; ++index) {
            pthread_join(*(threads + index), NULL);
        }
    }
    
end:
    free(threads);
    if (reg_infos) {
        for (index = 0; index < group_num; ++index) {
            reg_info_t *reg_info = reg_infos + index;
            free(reg_info->regs);
        }
        free(reg_infos);
    }
    return rt;
}

int process_results(line_t *lines, const char *match_file,
                    const char *nomatch_file) {
    line_t *head = lines;
    line_t *line = head->next;
    FILE   *fp_match = NULL;
    FILE   *fp_nomatch = NULL;
    int     rt = -1;
    
    fp_match = fopen(match_file, "wb");
    if (!fp_match) {
        goto end;
    }
    
    fp_nomatch = fopen(nomatch_file, "wb");
    if (!fp_nomatch) {
        goto end;
    }
    
    while (line) {
        /* last char is '\0', here we replace with '\n' */
        *(line->data + line->size) = '\n';
        fwrite(line->data, 1, line->size + 1,
               (line->match == 0 ? fp_match : fp_nomatch));
        line = line->next;
    }
    rt = 0;
    
end:
    fclose(fp_match);
    fclose(fp_nomatch);
    return rt;
}

//int main(int argc, char *argv[]) {
//    long     n_processor;
//    char    *sen_content = NULL;
//    char    *regs_content = NULL;
//    line_t  *reg_lines = NULL;
//    line_t  *sen_lines = NULL;
//    line_t  *sen_group = NULL;
//    int      reg_num, line_num;
//    int      rt = -1;
//    
//    const char *match_file = "/Users/chenliu/Desktop/match.txt";
//    const char *nomatch_file = "/Users/chenliu/Desktop/nomatch.txt";
//    const char *sen_file = "/Users/chenliu/Desktop/HalfYear_question.txt";
//    const char *reg_file = "/Users/chenliu/Desktop/regex_test.txt";
//    
//    n_processor = sysconf(_SC_NPROCESSORS_CONF);
//    printf("num processors %d\n", (int)n_processor);
//    
//    do {
//        sen_content = read_file(sen_file, NULL);
//        if (!sen_content) {
//            printf("fail to read %s\n", sen_file);
//            break;
//        }
//        
//        regs_content = read_file(reg_file, NULL);
//        if (!regs_content) {
//            printf("fail to read %s\n", reg_file);
//            break;
//        }
//        
//        reg_lines = parse_lines(regs_content, &reg_num);
//        if (!reg_lines) {
//            printf("fail to parse reg lines\n");
//            break;
//        }
//        printf("reg num:%d\n", reg_num);
//        
//        sen_lines = parse_lines(sen_content, &line_num);
//        if (!sen_lines) {
//            printf("fail to parse sen lines\n");
//            break;
//        }
//        printf("sen lines:%d\n", line_num);
//        
//        sen_group = splite_lines(sen_lines, line_num, n_processor);
//        if (!sen_group) {
//            printf("fail to splite sen lines\n");
//            break;
//        }
//        
//        rt = process_regxs(sen_group, (int)n_processor,
//                           reg_lines, reg_num);
//        if (rt != 0) {
//            printf("fail to process regxs\n");
//            break;
//        }
//        
//        rt = process_results(sen_lines, match_file, nomatch_file);
//    } while(0);
//    
//end:
//    free(sen_content);
//    free(regs_content);
//    free_lines(reg_lines);
//    free_lines(sen_lines);
//    free(sen_group);
//    printf("process end with %d\n", rt);
//}


