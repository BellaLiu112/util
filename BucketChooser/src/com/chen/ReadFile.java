package com.chen;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class ReadFile {
	private static ArrayList[] sampled_msg = new ArrayList[10];
	private static BucketChooser chooser = BucketChooser.create(10);

	public static void readFile(String filePath) throws FileNotFoundException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			throw e;
		}

		for (int i = 0; i < sampled_msg.length; ++i){
			sampled_msg[i] = new ArrayList();
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		String line = null;
		int idx;
		while (true) {
			try {
				line = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (line == null) {
				break;
			}
			idx = chooser.chooseBucket(line);
			sampled_msg[idx].add(line);

			System.out.println(line);
		}

		try {
			fis.close();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static BufferedWriter createBufferedWriter(String path) {
		FileOutputStream fos = null;
		try {
			File f = new File(path);
			if (!f.exists())
				f.createNewFile();
			fos = new FileOutputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (fos == null)
			return null;
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		return bw;
	}

	public static void main(String[] args) {

		String filePath = "//Users/chenliu/Desktop/nlp/HalfYear_question.txt";
		try {
			readFile(filePath);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BufferedWriter[] bws = new BufferedWriter[10];
		for (int i = 0; i < bws.length; ++i) {
			bws[i] = createBufferedWriter("/Users/chenliu/Desktop/nlp/HalfYear_" + i + ".txt");
		}

//		for (int i = 0; i < 1000; ++i) {
//			int which = i % 10;
//			try {
//				bws[which].write("12312123\n");
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

		for (int i = 0; i < sampled_msg.length; ++i){
			Iterator it = sampled_msg[i].iterator();
			while (it.hasNext()){
				String str = (String)it.next();
				try {
					bws[i].write(str + '\n');
				} catch (IOException e){
					e.printStackTrace();
				}
			}
		}


		
		for (int i = 0 ; i < bws.length; ++i) {
			try {
				bws[i].close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
