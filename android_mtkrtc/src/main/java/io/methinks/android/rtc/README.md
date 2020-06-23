## Initializing method differences

* repository initializing for local workspace.  

    1) clone repository to one lower level dir from where you want this sources place.  
    
    
    2) change cloned dir name what you want  
    
    
    
        thinker : 'android-mtkrtc-source' to 'mtkrtc'  
        rtc-sdk : 'android-mtk-rtc-source' to 'rtc'  
    
    
      
        
        

* apptest sdk

        1) put baseFeature value on MTKVideoChatClient builder.
            .baseFeature("apptest_sdk")


* thinker app

        1) put baseFeature value on MTKVideoChatClient builder.
            .baseFeature("thinker_app")


## how to import

* overwrite all of java code file to mtkrtc directory.