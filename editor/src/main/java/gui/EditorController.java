package gui;

import java.net.URL;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Stack;
import java.util.ResourceBundle;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javafx.concurrent.Worker;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javafx.collections.ListChangeListener;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.event.EventHandler;
import javafx.event.ActionEvent;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tab;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import netscape.javascript.JSObject;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.sun.javafx.webkit.WebConsoleListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.apache.commons.io.IOUtils;

import jiconfont.icons.FontAwesome;
import jiconfont.javafx.IconNode;

import gui.fileStructure.StructureTreeView;
import gui.fileBrowser.FileTreeItem;
import gui.fileBrowser.FileTreeView;
import gui.fileBrowser.FileWrapper;
import prover.TPTPDefinitions;
import prover.SystemOnTPTPProver;
import prover.LocalProver;
import gui.preferences.PreferencesController;
import gui.preferences.PreferencesModel;

public class EditorController implements Initializable {

    // ==========================================================================
    // FXML variables
    // ==========================================================================

    // DEBUG
    @FXML public void debugALG0157() { model.openStream(getClass().getResourceAsStream("/test/ALG015^7.p"), Paths.get("ALG015^7.p")); }
    @FXML public void debugCOM1601() { model.openStream(getClass().getResourceAsStream("/test/COM160^1.p"), Paths.get("COM160^1.p")); }
    @FXML public void debugLCL6331() { model.openStream(getClass().getResourceAsStream("/test/LCL633^1.p"), Paths.get("LCL633^1.p")); }
    @FXML public void debugLCL6341() { model.openStream(getClass().getResourceAsStream("/test/LCL634^1.p"), Paths.get("LCL634^1.p")); }
    @FXML public void debugSYN0001() { model.openStream(getClass().getResourceAsStream("/test/SYN000^1.p"), Paths.get("SYN000^1.p")); }
    @FXML public void debugSYN0002() { model.openStream(getClass().getResourceAsStream("/test/SYN000^2.p"), Paths.get("SYN000^2.p")); }
    // END DEBUG

    // Menu
    @FXML private Menu menubarRunProver;
    @FXML private Menu menubarFileReopenFile;
    @FXML private MenuItem menubarFileReopenFileNoFiles;

    // Toolbar
    @FXML private MenuButton toolbarRunProver;

    // Tabs left
    @FXML private SplitPane splitPaneVertical;
    @FXML private TabPane tabPaneLeft;
    @FXML private Tab tabPaneLeftCollapse;
    @FXML private Tab tabPaneLeftDummy;
    @FXML private FileTreeView fileBrowser;
    @FXML private StructureTreeView structureView;

    // Editor
    @FXML private TabPane thfArea;

    // Output
    @FXML public WebView outputWebView;

    // ==========================================================================
    // Controller Variables
    // ==========================================================================

    private static Logging log = Logging.getInstance();
    private EditorModel model;
    private Stage mainStage;
    private File dir;
    private Tab lastSelectedTabBeforeCollapse = null;
    static FontAwesome iconCollapse = FontAwesome.ANGLE_DOUBLE_DOWN;
    static FontAwesome iconUncollapse = FontAwesome.ANGLE_DOUBLE_UP;

    // ==========================================================================
    // Constructors / Init
    // ==========================================================================

    public EditorController(EditorModel model, Stage mainStage) {
        this.model = model;
        this.mainStage = mainStage;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // DEBUG
        WebConsoleListener.setDefaultListener(new WebConsoleListener(){
            @Override
            public void messageAdded(WebView webView, String message, int lineNumber, String sourceId) {
                System.out.println("Console: [" + sourceId + ":" + lineNumber + "] " + message);
            }
        });
        // END DEBUG

        // Pass some members on to the model
        this.model.thfArea = thfArea;

        // Initialize THF WebView
        EditorDocumentViewController emptyDoc = new EditorDocumentViewController(null, this.thfArea.getTabs());

        // Initialize Output WebView
        log.outputEngine = outputWebView.getEngine();
        log.outputEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>()
                {
                    @Override
                    public void changed(ObservableValue ov, Worker.State oldState, Worker.State newState)
                    {
                        if(newState == Worker.State.SUCCEEDED) {
                            log.init();

                            // DEBUG for proving history
                            /*ProvingHistory h = ProvingHistory.getInstance();
                            EditorDocument doc1 = new EditorDocument(Paths.get("/home/tg/d1.p"));
                            EditorDocument doc2 = new EditorDocument(Paths.get("/home/tg/d2.p"));
                            h.addDocument(doc1);
                            h.addDocument(doc2);
                            h.prove(doc1,"Satallax---3.2", Prover.ProverType.SYSTEMONTPTP_PROVER,5);
                            h.prove(doc1,"Satallax---3.2", Prover.ProverType.SYSTEMONTPTP_PROVER,5);
                            h.prove(doc2,"LEO-II---1.7.0", Prover.ProverType.SYSTEMONTPTP_PROVER,5);
                            System.out.println("######### entryList #########");
                            h.entryList.forEach(System.out::println);
                            System.out.println("######### documentToEntryListMap #########");
                            for (EditorDocument d : h.documentToEntryListMap.keySet()){
                                h.documentToEntryListMap.get(d).forEach(System.out::println);
                            }*/
                            // END DEBUG
                        }

                    }});

        try {
            log.outputEngine.loadContent(IOUtils.toString(getClass().getResourceAsStream("/gui/output.html"), "UTF-8"));
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }

        // Initialize recently opened files
        initializeListOfRecentlyOpenedFiles();

        // Initialize prover menu lists
        addCurrentlyAvailableProversToMenus();

        // Initialize tabs on the left side
        makeTabPaneCollapsable();

    }

    // ==========================================================================
    // Menu Name
    // ==========================================================================

    @FXML private void onNAMEHide(ActionEvent e) {
        // TODO
    }

    @FXML public void onNAMEPreferences(ActionEvent actionEvent) {
        Stage stage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/preferences.fxml"));
        loader.setControllerFactory(t->new PreferencesController(new PreferencesModel(), stage));
        Scene scene = null;
        try {
            scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void onNAMEExit(ActionEvent e) {
        // TODO
        System.exit(0);
    }

    // ==========================================================================
    // Menu File
    // ==========================================================================

    @FXML private void onFileNew(ActionEvent e) {
        // TODO
        System.out.println("newfile");
    }

    @FXML private void onFileOpenFile(ActionEvent e) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open thf file");
        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if(selectedFile == null)
            return;
        model.openFile(selectedFile);
    }

    private void initializeListOfRecentlyOpenedFiles() {
        model.recentlyOpenedFiles.addListener((ListChangeListener.Change<? extends String> c) -> {
            while (c.next()) {
                for (String fileToRemove : c.getRemoved()) {
                    Iterator<MenuItem> i = menubarFileReopenFile.getItems().iterator();
                    while (i.hasNext()) {
                        MenuItem item = i.next();
                        if (Objects.equals(item.getText(),fileToRemove)) {
                            i.remove();
                        }
                    }
                }
                for (String fileToAdd : c.getAddedSubList()) {
                    MenuItem item = new MenuItem(fileToAdd);
                    item.setOnAction(e->model.openFile(new File(fileToAdd)));
                    menubarFileReopenFile.getItems().add(2,item);
                }
            }
            // Add or remove "No recently opened files available" item as needed
            if (!menubarFileReopenFile.getItems().contains(menubarFileReopenFileNoFiles)) {
                if (model.recentlyOpenedFiles.isEmpty()) {
                    menubarFileReopenFile.getItems().add(menubarFileReopenFileNoFiles);
                }
            } else {
                if (!model.recentlyOpenedFiles.isEmpty()) {
                    menubarFileReopenFile.getItems().remove(menubarFileReopenFileNoFiles);
                }
            }
        });
        Config.getRecentlyOpenedFiles().stream().forEach(f->model.recentlyOpenedFiles.add(f));
    }

    @FXML private void clearRecentlyOpenedFilesList(ActionEvent e) {
        model.clearRecentlyOpenedFilesList();
    }

    @FXML private void onDirectoryOpen(ActionEvent e) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open directory");
        dir = directoryChooser.showDialog(mainStage);
        if(dir == null)
            return;
        //RootDirItem rootDirItem = ResourceItem.createObservedPath(dir.toPath());
        //fileBrowser.setRootDirectories(FXCollections.observableArrayList(rootDirItem));
        fileBrowser.openDirectory(dir);
        //model.openDirectory(dir);

        // Open file on double click
        fileBrowser.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent mouseEvent)
            {
                if(mouseEvent.getClickCount() == 2)
                {
                    Path path = getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), true, false);
                    if (path == null) {
                        return;
                    }
                    model.openFile(new File(path.toString()));
                }
            }
        });

        final ContextMenu contextMenu = new ContextMenu();
        MenuItem newFile = new MenuItem("New file");
        MenuItem newDirectory = new MenuItem("New directory");
        MenuItem copy = new MenuItem("Copy");
        MenuItem copyDirName = new MenuItem("Copy directory name");
        MenuItem copyPath = new MenuItem("Copy path to clipboard");
        MenuItem copyRelPath = new MenuItem("Copy relative path to clipboard");
        MenuItem rename = new MenuItem("Rename");
        MenuItem paste = new MenuItem("Paste");
        MenuItem delete = new MenuItem("Delete");
        contextMenu.getItems().addAll(newFile, newDirectory, copy, copyDirName, copyPath, copyRelPath, rename, paste, delete);

        final ContextMenu contextMenuFile = new ContextMenu();
        MenuItem copyFile = new MenuItem("Copy");
        MenuItem copyFileName = new MenuItem("Copy file name");
        MenuItem copyPathFile = new MenuItem("Copy path to clipboard");
        MenuItem copyRelPathFile = new MenuItem("Copy relative path to clipboard");
        MenuItem copyContent = new MenuItem("Copy file content to clipboard");
        MenuItem renameFile = new MenuItem("Rename");
        MenuItem pasteFile = new MenuItem("Paste");
        MenuItem deleteFile = new MenuItem("Delete");
        MenuItem runProver = new MenuItem("Run prover");
        contextMenuFile.getItems().addAll(copyFile, copyFileName, copyPathFile, copyRelPathFile, copyContent, renameFile, pasteFile, deleteFile, runProver);

        runProver.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Run prover ...");
            }
        });
        
        newFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                
                String error = "";
                String name = null;
                Path directory = null;
                
                while (true) {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("New file");
                    dialog.setHeaderText("New file name"+error);
                    dialog.setContentText("Please enter the file name:");
                    
                    Optional<String> result = dialog.showAndWait();
                    if (!result.isPresent())
                        break;
                    name = result.get();
                    if (name.equals("") || name == null) {
                        error = "\n\nERROR: Please enter a file name!";
                        System.out.println("A");
                        continue;
                    }
                    if (name.contains("../") || name.contains("..\\")) {
                        error = "\n\nERROR: Please enter a valid file name!";
                        continue;
                    }
                    directory = getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), false, false);
                    Path newFile = directory.resolve(name);
                    File file = new File(newFile.toString());
                    try {
                        if (file.createNewFile()) {
                            FileTreeItem item = new FileTreeItem(new FileWrapper(file));
                            item.setGraphic(item.getIconNodeByFile(file));
                            fileBrowser.getSelectionModel().getSelectedItem().getChildren().add(item);
                            ((FileTreeItem) fileBrowser.getSelectionModel().getSelectedItem()).sortChildren(false);
                            //FileTreeView.updateTree(fileBrowser.getSelectionModel().getSelectedItem(), fileBrowser.getRoot(), new File(directory.toString()));
                            break;
                        } else {
                            continue;
                        }
                    } catch (IOException e) {
                        error = "\n\nA file with this name already exists or the file name is invalid";
                        continue;
                    }
                }
            }
        });
        newDirectory.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                
                String error = "";
                String name = null;
                Path directory = null;
                
                while (true) {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("New directory");
                    dialog.setHeaderText("New directory name"+error);
                    dialog.setContentText("Please enter the directory name:");
                    
                    Optional<String> result = dialog.showAndWait();
                    if (!result.isPresent())
                        break;
                    name = result.get();
                    if (name.equals("") || name == null) {
                        error = "\n\nERROR: Please enter a directory name!";
                        System.out.println("A");
                        continue;
                    }
                    if (name.contains("/") || name.contains("\\")) {
                        error = "\n\nERROR: Please enter a valid directory name!";
                        continue;
                    }
                    directory = getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), false, false);
                    Path newDir = directory.resolve(name);
                    File file = new File(newDir.toString());
                    
                    if (file.mkdir()) {
                        FileTreeItem item = new FileTreeItem(new FileWrapper(file));
                        item.setGraphic(item.getIconNodeByFile(file));
                        fileBrowser.getSelectionModel().getSelectedItem().getChildren().add(item);
                        ((FileTreeItem) fileBrowser.getSelectionModel().getSelectedItem()).sortChildren(false);
                        break;
                    } else {
                        continue;
                    }
                }
            }
        });
        
        renameFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                renameFileOrDir();
            }
        });
        rename.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                renameFileOrDir();
            }
        });
        
        pasteFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copyFile(false);
            }
        });
        paste.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copyFile(true);
            }
        });
        
        // Copy file to clipboard.
        // TODO: File is only available within our application!
        copyFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copyFileOrDirToClipboard();
            }
        });
        copy.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copyFileOrDirToClipboard();
            }
        });
        copyFileName.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copyFileOrDirectoryName();
            }
        });
        copyDirName.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                copyFileOrDirectoryName();
            }
        });

        // Copy file content to clipboard
        copyContent.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                File file = new File(getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), true, false).toString());
                InputStream stream = null;
                try {
                    stream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    log.error(e.getMessage());
                }
                if (stream != null) {
                    try {
                        copyStringToClipboard(IOUtils.toString(stream, "UTF-8"));
                    } catch (IOException e1) {
                        log.error(e1.getMessage());
                    }
                }
            }
        });

        copyPath.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                copyFilePathToClipboard(false);
            }
        });

        copyPathFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                copyFilePathToClipboard(false);
            }
        });

        copyRelPathFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                copyFilePathToClipboard(true);
            }
        });

        copyRelPath.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent event) {
                copyFilePathToClipboard(true);
            }
        });

        // Delete file in file browser.
        deleteFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TreeItem<FileWrapper> item = fileBrowser.getSelectionModel().getSelectedItem();
                File file = new File(getPathToSelectedItem(item, true, false).toString());

                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Delete file");
                alert.setHeaderText("Delete file?");
                alert.setContentText("Do you really want to delete the file "+item.getValue().toString()+"?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    file.delete();
                    item.getParent().getChildren().remove(item);
                }
            }
        });
        
        delete.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TreeItem<FileWrapper> item = fileBrowser.getSelectionModel().getSelectedItem();
                File file = new File(getPathToSelectedItem(item, false, false).toString());

                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Delete folder");
                alert.setHeaderText("Delete folder?");
                alert.setContentText("Do you really want to delete the folder "+item.getValue().toString()+"?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK){
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(file);
                        item.getParent().getChildren().remove(item);
                    } catch (IOException e) {
                        Alert alert1 = new Alert(AlertType.ERROR);
                        alert1.setTitle("ERROR");
                        alert1.setHeaderText("Error");
                        alert1.setContentText("There was an error deleting the folder "+file.getName()+"!");
                        alert1.showAndWait();
                    }
                }
            }
        });

        fileBrowser.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            if (fileBrowser.getSelectionModel().getSelectedItem() == null) {
                // Do nothing if nothing is selected.
            } else if (fileBrowser.getSelectionModel().getSelectedItem().isLeaf()) {
                contextMenuFile.show(fileBrowser, event.getScreenX(), event.getScreenY());
            }
            else {
                contextMenu.show(fileBrowser, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });
        fileBrowser.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            contextMenu.hide();
            contextMenuFile.hide();
        });
    }

    @FXML private void onFileSave(ActionEvent e) {
        // TODO
    }

    @FXML private void onFileSaveAs(ActionEvent e) {
        // TODO
    }

    @FXML private void onFileClose(ActionEvent e) {
        // TODO
        System.exit(0);
    }

    /**
     * This method returns the path to the selected item in the file browser.
     * @param selectedItem: selected item in the file browser.
     * @param onlyLeavesAllowed: if true, only the path of leaves is returned.
     * @param relativePath: if true, the path of the selected item relative to the root of the file browser is returned.
     * @return the path of the selected item.
     */
    private Path getPathToSelectedItem(TreeItem<FileWrapper> selectedItem, Boolean onlyLeavesAllowed, Boolean relativePath) {
        if ( selectedItem == null || onlyLeavesAllowed && !selectedItem.isLeaf()) {
            return null;
        }
        if (selectedItem.getParent() == null) {
            return dir.toPath();
        }
        Path root;
        if (relativePath) {
            root = Paths.get("");
        } else {
            root = dir.toPath();
        }
        LinkedList<String> paths = new LinkedList<String>();
        paths.add(selectedItem.getValue().toString());
        while (selectedItem.getParent() != null) {
            selectedItem = selectedItem.getParent();
            if (selectedItem.getParent() != null)   // Necessary because the root directory is already in "root".
                paths.add(selectedItem.getValue().toString());
        }

        while (paths.size() > 0) {
            root = root.resolve(paths.pollLast());
        }
        return root;
    }

    /**
     * Copy path of selected item in file browser to system clipboard.
     * @param relativePath: copy the path relative to the root of the file browser. Otherwise, the absolute path is copied.
     */
    private void copyFilePathToClipboard(Boolean relativePath) {
        Path path = getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), false, relativePath);
        copyStringToClipboard(path.toString());
    }

    /**
     * Copy @param string to system clipboard.
     */
    private void copyStringToClipboard(String string) {
        ClipboardContent content = new ClipboardContent();
        content.putString(string);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        //StringSelection selection = new StringSelection(string);
        //clipboard.setContents(selection, selection);
        clipboard.setContent(content);
    }

    /**
     * Copy the name of the selected file or directory in the file browser to the clipboard.
     */
    private void copyFileOrDirectoryName() {
        TreeItem<FileWrapper> item = fileBrowser.getSelectionModel().getSelectedItem();
        if (item != null) {
            copyStringToClipboard(item.getValue().toString());
        }
    }
    
    private void copyFileOrDirToClipboard() {
        Path path = getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), false, false);
        
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        
        final ClipboardContent content = new ClipboardContent();
        ArrayList<File> fileList = new ArrayList<File>();
        fileList.add(new File(path.toString()));
        content.putFiles(fileList);
        clipboard.setContent(content);
    }
    
    /**
     * Copy file from clipboard to the selected position in the file browser.
     * @param selectedItemIsDirectory
     */
    private void copyFile(boolean selectedItemIsDirectory) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        List<File> files = clipboard.getFiles();
        if (files == null || files.size() == 0) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No file to paste");
            alert.setHeaderText("No file to paste");
            alert.setContentText("There is no file in the clipboard!");
            alert.showAndWait();
            return;
        }
        
        File f = null;
        
        if (selectedItemIsDirectory) {
            f = new File(getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), false, false).toString());
        } else {
            f = new File(getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem().getParent(), false, false).toString());
        }
        
        for (File file : files) {
            try {
                File destination = new File (f.toPath().resolve(file.getName()).toString());
                
                if (!file.isDirectory() && destination.exists() && !destination.isDirectory()) {
                    String error = "";
                    String name = file.getName();
                    while (true) {
                        TextInputDialog dialog = new TextInputDialog(name);
                        dialog.setTitle("Copy");
                        dialog.setHeaderText("Copy file:"+error);
                        dialog.setContentText("The file "+name+" already exists. Please enter a new name or replace the existing file:");
                        
                        Optional<String> result = dialog.showAndWait();
                        if (!result.isPresent())
                            return;
                        name = result.get();
                        if (name.equals("") || name == null) {
                            error = "\n\nERROR: Please enter a file name!";
                            continue;
                        }
                        if (name.contains("../") || name.contains("..\\")) {
                            error = "\n\nERROR: Please enter a valid file name!";
                            continue;
                        }
                        destination = new File(f.toPath().resolve(name).toString());
                        break;
                    }
                }
                
                boolean noNewFileBrowserEntry = false;
                
                if (!file.isDirectory() && destination.exists() && !destination.isDirectory() || file.isDirectory() && destination.exists() && destination.isDirectory()) {
                    noNewFileBrowserEntry = true;
                }
                
                if (!file.isDirectory()) {
                    Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    destination = f;
                    org.apache.commons.io.FileUtils.copyDirectoryToDirectory(file, destination);
                    destination = new File (f.toPath().resolve(file.getName()).toString());
                }
                
                if (!noNewFileBrowserEntry) {
                    FileTreeItem item = new FileTreeItem(new FileWrapper(destination));
                    item.setGraphic(item.getIconNodeByFile(destination));
                    if (selectedItemIsDirectory) {
                        fileBrowser.getSelectionModel().getSelectedItem().getChildren().add(item);
                    } else {
                        fileBrowser.getSelectionModel().getSelectedItem().getParent().getChildren().add(item);
                    }
                }
            } catch (IOException e) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("ERROR");
                alert.setHeaderText("Error");
                alert.setContentText("There was an error pasting the file "+file.getName()+"!");
                alert.showAndWait();
            } finally {
                if (selectedItemIsDirectory) {
                    ((FileTreeItem) fileBrowser.getSelectionModel().getSelectedItem()).sortChildren(false);
                } else {
                    ((FileTreeItem) fileBrowser.getSelectionModel().getSelectedItem().getParent()).sortChildren(false);
                }
            }
        }
    }
    
    public void renameFileOrDir() {
        File source = new File(getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem(), false, false).toString());
        String error = "";
        String name = null;
        Path directory = null;
        
        if (fileBrowser.getSelectionModel().getSelectedItem().getParent() == null)
            return;
        
        while (true) {
            TextInputDialog dialog = new TextInputDialog(source.getName());
            dialog.setTitle("Rename file");
            dialog.setHeaderText("New file name"+error);
            dialog.setContentText("Please enter the new file name:");
            
            Optional<String> result = dialog.showAndWait();
            if (!result.isPresent())
                break;
            name = result.get();
            if (name.equals("") || name == null) {
                error = "\n\nERROR: Please enter a file name!";
                continue;
            }
            if (name.contains("../") || name.contains("..\\")) {
                error = "\n\nERROR: Please enter a valid file name!";
                continue;
            }
            directory = getPathToSelectedItem(fileBrowser.getSelectionModel().getSelectedItem().getParent(), false, false);
            Path newFile = directory.resolve(name);
            File file = new File(newFile.toString());
            if (source.renameTo(file)) {
                FileTreeItem item = new FileTreeItem(new FileWrapper(file));
                TreeItem<FileWrapper> sourceItem = fileBrowser.getSelectionModel().getSelectedItem();
                sourceItem.getParent().getChildren().remove(sourceItem);
                item.setGraphic(item.getIconNodeByFile(file));
                fileBrowser.getSelectionModel().getSelectedItem().getParent().getChildren().add(item);
                ((FileTreeItem) fileBrowser.getSelectionModel().getSelectedItem().getParent()).sortChildren(false);
                //FileTreeView.updateTree(fileBrowser.getSelectionModel().getSelectedItem(), fileBrowser.getRoot(), new File(directory.toString()));
                break;
            } else {
                continue;
            }
        }
    }

    // ==========================================================================
    // Menu Edit
    // ==========================================================================

    // ==========================================================================
    // Menu View
    // ==========================================================================

    @FXML public void onViewToolWindowProject(ActionEvent actionEvent){
        // TODO
    }

    @FXML public void onViewIncreaseFontSize(ActionEvent actionEvent) {
        model.onViewIncreaseFontSize();
    }

    @FXML public void onViewDecreaseFontSize(ActionEvent actionEvent) {
        model.onViewDecreaseFontSize();
    }

    @FXML public void onViewDefaultFontSize(ActionEvent actionEvent) {
        model.onViewDefaultFontSize();
    }

    @FXML public void onViewEnterPresentationMode(ActionEvent actionEvent) {
        model.onViewEnterPresentationMode();
    }

    // ==========================================================================
    // Menu Prover
    // ==========================================================================

    private void addCurrentlyAvailableProversToMenus() {
        try {
            List<String> availableProversLocal = LocalProver.getInstance().getAvailableProvers(TPTPDefinitions.TPTPDialect.THF);
            List<String> availableProversRemote = SystemOnTPTPProver.getInstance().getAvailableDefaultProvers(TPTPDefinitions.TPTPDialect.THF);
            ToggleGroup menubarProvers = new ToggleGroup();
            // addDocument list of local provers to menubar
            MenuItem labelForLocalProvers = new MenuItem("Local provers");
            labelForLocalProvers.setDisable(true);
            menubarRunProver.getItems().add(labelForLocalProvers);
            if (availableProversLocal.isEmpty()) {
                MenuItem noLocalProvers = new MenuItem("No local provers available");
                noLocalProvers.setDisable(true);
                menubarRunProver.getItems().add(noLocalProvers);
            } else {
                for (Iterator<String> i = availableProversLocal.iterator(); i.hasNext();) {
                    RadioMenuItem item = new RadioMenuItem(i.next().replace("---"," "));
                    item.setToggleGroup(menubarProvers);
                    menubarRunProver.getItems().add(item);
                }
            }
            SeparatorMenuItem separator = new SeparatorMenuItem();
            menubarRunProver.getItems().add(separator);
            // addDocument list of remote provers to menubar
            MenuItem labelForRemoteProvers = new MenuItem("Remote provers");
            labelForRemoteProvers.setDisable(true);
            menubarRunProver.getItems().add(labelForRemoteProvers);
            if (availableProversRemote.isEmpty()) {
                MenuItem noRemoteProvers = new MenuItem("No remote provers available");
                noRemoteProvers.setDisable(true);
                menubarRunProver.getItems().add(noRemoteProvers);
            } else {
                for (Iterator<String> i = availableProversRemote.iterator(); i.hasNext();) {
                    RadioMenuItem item = new RadioMenuItem(i.next().replace("---"," "));
                    item.setToggleGroup(menubarProvers);
                    menubarRunProver.getItems().add(item);
                }
            }

            // listener for the toolbar prover menu
            toolbarRunProver.showingProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if(newValue) {
                        toolbarRunProver.getItems().clear();
                        ToggleGroup toolbarProvers = new ToggleGroup();
                        // addDocument list of local provers to toolbar
                        for (Iterator<String> i = availableProversLocal.iterator(); i.hasNext();) {
                            RadioMenuItem item = new RadioMenuItem(i.next().replace("---"," "));
                            item.setToggleGroup(toolbarProvers);
                            toolbarRunProver.getItems().add(item);
                        }
                        // addDocument list of remote provers to toolbar
                        for (Iterator<String> i = availableProversRemote.iterator(); i.hasNext();) {
                            RadioMenuItem item = new RadioMenuItem(i.next().replace("---"," "));
                            item.setToggleGroup(toolbarProvers);
                            toolbarRunProver.getItems().add(item);
                        }
                    }
                }
            });
        } catch (IOException e) {
            // TODO: write log entry
        }
    }

    // ==========================================================================
    // Menu Help
    // ==========================================================================
    @FXML private void onHelpAbout(ActionEvent e){

    }

    // ==========================================================================
    // Toolbar
    // ==========================================================================

    @FXML private void onTestPref(ActionEvent e)
    {
    }

    @FXML
    private void onReparse(ActionEvent e)
    {
    }

    @FXML
    private void onPrintTree(ActionEvent e)
    {
    }

    // ==========================================================================
    // Tabs left
    // ==========================================================================

    private void makeTabPaneCollapsable() {
        IconNode icon = new IconNode(iconCollapse);
        icon.getStyleClass().add("tabpane-icon");
        tabPaneLeftCollapse.setGraphic(icon);

        tabPaneLeft.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
                if (oldTab != tabPaneLeftCollapse) {
                    double orgDividerPosition = splitPaneVertical.getDividers().get(0).positionProperty().getValue();
                    double minDividerPosition = getMinDividerPosition();
                    if(newTab == tabPaneLeftCollapse) {
                        if (minDividerPosition/orgDividerPosition<0.95) {
                            lastSelectedTabBeforeCollapse = oldTab;
                            splitPaneVertical.setDividerPosition(0,minDividerPosition+(1/splitPaneVertical.getWidth()));
                            tabPaneLeft.getSelectionModel().select(tabPaneLeftDummy);
                        } else {
                            splitPaneVertical.setDividerPosition(0,0.2);
                            tabPaneLeft.getSelectionModel().select(lastSelectedTabBeforeCollapse);
                        }
                    } else if (newTab != tabPaneLeftCollapse && newTab != tabPaneLeftDummy) {
                        if (minDividerPosition/orgDividerPosition>=0.95) {
                            splitPaneVertical.setDividerPosition(0,0.2);
                        }
                    }
                }
            }
        });

        splitPaneVertical.getDividers().get(0).positionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                IconNode icon;
                if(getMinDividerPosition()/(double)newValue<0.95) {
                    icon = new IconNode(iconCollapse);
                    splitPaneVertical.setResizableWithParent(tabPaneLeft, Boolean.TRUE);
                } else {
                    icon = new IconNode(iconUncollapse);
                    splitPaneVertical.setResizableWithParent(tabPaneLeft, Boolean.FALSE);
                }
                icon.getStyleClass().add("tabpane-icon");
                tabPaneLeftCollapse.setGraphic(icon);
            }
        });

        tabPaneLeft.getSelectionModel().select(2);

    }

    private double getMinDividerPosition() {
        return tabPaneLeft.getTabMaxHeight()/splitPaneVertical.getWidth();
    }

}
