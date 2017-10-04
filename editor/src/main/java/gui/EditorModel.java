package gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class EditorModel
{
    private static Logging log = Logging.getInstance();

    public TabPane thfArea;

    public ObservableList<String> recentlyOpenedFiles;

    public EditorModel()
    {
        recentlyOpenedFiles = FXCollections.observableArrayList(); // first element = oldest file, last element = latest file
    }

    protected EditorDocumentViewController getSelectedTab()
    {
        if (thfArea.getSelectionModel().getSelectedItem() == null) return null;
        else return (EditorDocumentViewController) thfArea.getSelectionModel().getSelectedItem().getUserData();
    }

    /**
     * Updates concerning recently opened files
     * Is called after opening a file
     * @param file
     */
    private void updateRecentlyOpenedFiles(File file){
        recentlyOpenedFiles.remove(file.getAbsolutePath());
        recentlyOpenedFiles.add(file.getAbsolutePath());
        if (recentlyOpenedFiles.size() > Config.maxRecentlyOpenedFiles) recentlyOpenedFiles.remove(0);
        Config.setRecentlyOpenedFiles(recentlyOpenedFiles);
    }

    public void clearRecentlyOpenedFilesList(){
        recentlyOpenedFiles.clear();
        Config.setRecentlyOpenedFiles(recentlyOpenedFiles);
    }

    public void onViewIncreaseFontSize() {
        getSelectedTab().model.onViewIncreaseFontSize();
    }

    public void onViewDecreaseFontSize() {
        getSelectedTab().model.onViewDecreaseFontSize();
    }

    public void onViewEnterPresentationMode() {
        getSelectedTab().model.onViewEnterPresentationMode();
        // TODO close side drawer, ...
    }

    public void onViewDefaultFontSize()
    {
        getSelectedTab().model.onViewDefaultFontSize();
    }

    private EditorDocumentViewController getNewTab() {
        EditorDocumentViewController doc;

        doc = new EditorDocumentViewController(null, this.thfArea.getTabs());
        thfArea.getSelectionModel().select(doc.tab);

        return doc;
    }

    public void openStream(InputStream stream, Path path) {
        getNewTab().model.openStream(stream, path);
    }

    public void openFile(File file) {
        Tab openedTab = null;
        for (Tab t : thfArea.getTabs()){
            if (((EditorDocumentViewController) t.getUserData()).model.getPath().equals(file.toPath())){
                openedTab = t;
                break;
            }
        }
        if (openedTab != null){
            thfArea.getSelectionModel().select(openedTab);
            log.warning("File already opened. File='" + file + "'.");
            return;
        }
        getNewTab().model.openFile(file);
        updateRecentlyOpenedFiles(file);
        log.info("Opened file='" + file + "'.");
    }

    public void saveFile(EditorDocumentModel m){
        try {
            Files.write(m.getPath(),m.getText().getBytes());
            log.info("Saved editor content to file " + m.getPath() + ".");
        } catch (IOException e1) {
            e1.printStackTrace();
            log.error("Could not save editor content to file " + m.getPath() + ".");
        }
    }

    public void newFile(){
        getNewTab();
    }

    // ------- DEBUG FUNCTIONS -------
    public void debugPrintHTML() {
        getSelectedTab().model.debugPrintHTML();
    }
}
