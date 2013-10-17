package com.globant.mobile.handson.media.task;

public interface WorkerListener<T> {
	
	void onTaskCompleted(T result);

}
