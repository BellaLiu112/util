package com.chen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class BucketProcessor {
	private static BucketChooser mChooser;
	private BufferedWriter[] mBws;
	private String mSrcFile;
	private String[] mDstFiles;
	
	public BucketProcessor(int numBucket, String srcFile, String[] dstFiles) {
		mChooser = BucketChooser.create(numBucket);
		mBws = new BufferedWriter[numBucket];
		this.mSrcFile = srcFile;
		this.mDstFiles = dstFiles;

	}
	
	public void process() throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(mSrcFile);
		} catch (FileNotFoundException e) {
			throw e;
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		for (int i = 0 ; i < mDstFiles.length; ++i) {
			mBws[i] = createBufferedWriter(mDstFiles[i]);
		}
		
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
			
			idx = mChooser.chooseBucket(line);
			
			try {
				mBws[idx].write(line);
				mBws[idx].write("\n");
			} catch (IOException e) {
				e.printStackTrace();
			}

			//System.out.println(line);
		}

		try {
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (int i = 0 ; i < mDstFiles.length; ++i) {
			mBws[i].close();
			mBws[i] = null;
		}
	}

	public static BufferedWriter readecreateBufferedWriter(String path) {
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

		//String workspace = "//Users/chenliu/Desktop/nlp";
		String workspace = "C:/Users/XELEMENTLIU/Documents/WeChat Files/justproxyme/Files";
		String srcFile = workspace + "/HalfYear_question.txt";
		int numBuckets = 10;
		String[] dstFiles = new String[numBuckets];
		BufferedWriter[] bws = new BufferedWriter[numBuckets];
		for (int i = 0; i < bws.length; ++i) {
			dstFiles[i] = workspace + "/HalfYear_" + i + ".txt";
		}
       
		 BucketProcessor processor = new BucketProcessor(numBuckets, srcFile, dstFiles);
		try {
			processor.process();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
