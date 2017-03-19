package com.xskin.avisos;

/**
 * Created by bort0 on 18/03/2017.
 */

public class Aviso {
    private int mId;
    private String mContent;
    private int mImportant;

    public Aviso(int id, String content, int important) {
        mId = id;
        mImportant = important;
        mContent = content;
    }

    public int getId() {
        return mId;
    }
    public void setId(int id) {
        mId = id;
    }
    public int getImportant() {
        return mImportant;
    }
    public void setImportant(int important) {
        mImportant = important;
    }
    public String getContent() {
        return mContent;
    }
    public void setContent(String content) {
        mContent = content;
    }
}
