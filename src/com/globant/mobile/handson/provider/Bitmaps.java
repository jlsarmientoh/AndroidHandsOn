package com.globant.mobile.handson.provider;

public class Bitmaps {

	public static String[] imageUrls = new String[0];
	public static String[] imageThumbUrls = new String[0];
	
	public static void removeItemAt(int index){
		String[] tmpImageUrls = new String[imageUrls.length - 1];
		String[] tmpImageThumbUrls = new String[imageThumbUrls.length - 1];
		
		for(int i = 0, j= 0; i < tmpImageUrls.length; i++){
			if(i != index){
				tmpImageUrls[i] = imageUrls[j];
				tmpImageThumbUrls[i] = imageThumbUrls[j];
				j++;
			}else{
				j++;
				tmpImageUrls[i] = imageUrls[j];
				tmpImageThumbUrls[i] = imageThumbUrls[j];
			}
		}
		
		imageUrls = tmpImageUrls;
		imageThumbUrls = tmpImageThumbUrls;
	}
}
