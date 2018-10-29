package com.photobooth.photobooth.util;

import java.io.File;

import android.content.Context;

public class FileCache {
    
    private File cacheDir;
    private boolean localFileCache;
    
    public FileCache(Context context, boolean localCache) {
    	this.localFileCache = localCache;
        //Find the dir to save cached images
    	if(localFileCache) {
    		cacheDir = new File("/data/data/com.photobooth.photobooth/Files/imgaes");
    	} else {
    		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
                cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"PhotoBoothCache");
            else
                cacheDir=context.getCacheDir();
    	}
    	if(!cacheDir.exists()) {
          boolean created = cacheDir.mkdirs();
          if(created)
        	  Util.LOG("image loader folder created : " + cacheDir.getAbsolutePath());
          else
        	  Util.LOG("image loader folder not created");
    	}
    }
    
    public File getFile(String url){
        //I identify images by hashcode. Not a perfect solution, good for the demo.
        String filename=String.valueOf(url.hashCode());
        //Another possible solution (thanks to grantland)
        //String filename = URLEncoder.encode(url);
        File f;
        //if(localFileCache)
        //	f = new File("file:///" + cacheDir.getAbsolutePath(), filename);
        //else
        	f = new File(cacheDir, filename);
        return f;
        
    }
    
    public void clear(){
        File[] files=cacheDir.listFiles();
        if(files==null)
            return;
        for(File f:files)
            f.delete();
    }

}