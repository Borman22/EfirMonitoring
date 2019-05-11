package tv.sonce.efirmonitoring.model;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class FolderKeeper {

    private boolean isFolderExist = false;
    private final String pathToFolder;
    private File folder;
    private File[] arrayOfFiles;
    private Map<String, File> mapOfFiles;
    private Set<String> setOfFileNames;

    public FolderKeeper(String pathToFolder) throws FileNotFoundException {
        this.pathToFolder = pathToFolder;
        refresh();
    }

    public void refresh() throws FileNotFoundException {
        isFolderExist = false;
        folder = new File(pathToFolder);
        arrayOfFiles = folder.listFiles();
        if(arrayOfFiles == null)
            throw new FileNotFoundException("Не удалось подключиться к папке " + pathToFolder);

        mapOfFiles = new HashMap<>(3000);
        for (int i = 0; i < arrayOfFiles.length; i++)
            mapOfFiles.put(arrayOfFiles[i].getName(), arrayOfFiles[i]);

        setOfFileNames = new HashSet<>(mapOfFiles.keySet());
        isFolderExist = true;
    }

    public List<String> syncronizeFrom(FolderKeeper originFolder) throws FileNotFoundException {
        if(originFolder == null)
            throw new FileNotFoundException("Аргумент функции syncronizeFrom(FolderKeeper originFolder) не может быть равен null");
        if(!isFolderExist)
            throw new FileNotFoundException(pathToFolder + " не существует");
        if(!originFolder.isFolderExist)
            throw new FileNotFoundException(originFolder.pathToFolder + " не существует или не удалось получить к ней доступ");

//        this.refresh();
//        originFolder.refresh();

        List<String> listOfCopiedFiles = new ArrayList<>();

        for (Map.Entry<String, File> entryOrigin : originFolder.mapOfFiles.entrySet()) {
            if (!setOfFileNames.contains(entryOrigin.getKey())) {
                if (!entryOrigin.getValue().isDirectory()) {
                    try {
                        FileUtils.copyFileToDirectory(entryOrigin.getValue(), folder);
                        listOfCopiedFiles.add(entryOrigin.getKey());
                    } catch (IOException e) {
                        throw new FileNotFoundException("Не удалось скопировать файл " + entryOrigin.getValue() + " в директорию " + pathToFolder + "/n" + e.getMessage());
                    }
                }
            }
        }
        refresh();
        return listOfCopiedFiles;
    }

    public List<String> getAbsentFilesList(String[] arrayBeginningOfFileNames){
        List<String> listOfAbsentFiles = new ArrayList<>();

         for (String beginningOfFileName : arrayBeginningOfFileNames) {
             LabelA:
             {
                 for (String str : setOfFileNames)
                     if (str.startsWith(beginningOfFileName))
                         break LabelA;
                 listOfAbsentFiles.add(beginningOfFileName);
             }
         }
        return listOfAbsentFiles;
    }

    @Override
    public String toString() {
        return pathToFolder;
    }
}
