package com.example.factorial.src;

import java.util.ArrayList;
public class PageManagement {
    private PageManagement(){

    }
    static PageManagement Instance;
    static public PageManagement GetSingleton(){
        if(Instance==null){
            Instance = new PageManagement();
        }
        return Instance;
    }
}
