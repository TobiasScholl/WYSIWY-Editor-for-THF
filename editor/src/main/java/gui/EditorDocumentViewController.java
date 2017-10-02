package gui;

import java.io.IOException;

import java.nio.file.Path;

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javafx.concurrent.Worker;

import javafx.scene.control.Tab;
import javafx.scene.web.WebView;

import org.apache.commons.io.IOUtils;

import netscape.javascript.JSObject;

import gui.EditorDocument;
import gui.EditorDocumentModel;

public class EditorDocumentViewController
{
    private List<Tab> tabs;
    public Tab tab;
    public WebView editor;

    public EditorDocument doc;
    public EditorDocumentModel model;

    public EditorDocumentViewController(Path path, List<Tab> tabs)
    {
        this.editor = new WebView();
        this.editor.getEngine().setJavaScriptEnabled(true);

        this.tab = new Tab();
        if(path == null)
            this.tab.setText("unnamed");
        else
            this.tab.setText(path.getFileName().toString());
        this.tab.setContent(this.editor);
        this.tab.setUserData(this);

        this.tabs = tabs;
        this.tabs.add(this.tab);

        this.tab.setOnCloseRequest(
            e ->
            {
                /* Don't close the last tab. */
                if(this.tabs.size() <= 1)
                {
                    if(this.model != null)
                    {
                        this.model = new EditorDocumentModel(editor.getEngine(), EditorDocumentViewController.this);
                    }

                    e.consume();
                }
            }
        );

        /* TODO: Merge these two. */
        this.model = new EditorDocumentModel(editor.getEngine(), EditorDocumentViewController.this);
        this.doc = new EditorDocument(path);
    }

    public void setText(String text)
    {
        this.tab.setText(text);
    }
}
