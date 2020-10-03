# Zip Editor Documentation
The Zip Editor is an Eclipse plugin that allows to view and modify archives.

## Content
* [Archive Types](#archive-types)
* [Editor](#editor)
    * [View mode](#view-mode)
    * [Open files internally](#open-files-internally)
    * [Use external editors or commands](#use-external-editors-or-commands)
    * [Modify internally opened files](#modify-internally-opened-files)
    * [Properties of archive entries](#properties-of-archive-entries)
* [Project Explorer Integration](#project-explorer-integration)
* [Archive Search](#archive-search)
    * [Search within the current file](#search-within-the-current-file)
    * [Search within workspace files](#search-within-workspace-files)
    * [Search within libraries](#search-within-libraries)
    * [Search within external files](#search-within-external-files)
    * [Work with the search result view](#work-with-the-search-result-view)

### Archive Types
These archive types are supported:

* Zip
* Tar
* Tar-Gzip
* Tar-Bzip2
* RPM (readonly)

### Editor
The main functionality of the plugin is the Zip Editor itself. It is by default associated with these archive extensions mapped via the respective archive content type:
* Zip (*.aar, *.apk, *.ear, *.jar, *.jmod, *.war, *.zip)
* Tar (*.tar)
* Tar gz (*.tar.gz, *.tgz)
* Tar Bzip2 (*.tar.bz2, *.tbz)
* Gzip (*.gz)
* Bzip2 (*.bz2)*
* Rpm (*.rpm)

With the Zip Editor you can open each supported archive file regardless if the file is located within the Eclipse workspace or not. It can also open files from other sources like a remote repository. Such files normally are opened readonly.

#### View mode
The editor has two supported modes: table (default) and tree. You can toggle between them using the toolbar item ![](https://sourceforge.net/p/zipeditor/zipeditor/ci/master/tree/ZipEditor/icons/togglemode.gif?format=raw).

In table mode you can choose the visible columns using the toolbar menu ![](https://sourceforge.net/p/zipeditor/zipeditor/ci/master/tree/ZipEditor/icons/arrow_down.gif?format=raw). There are different columns depending on the archive type, e.g. for a Zip archive you can choose to show the compression method (stored or deflated) or for a Tar archive you can choose to show the file mode.

#### Open files internally
If the editor has been opened on a file, you can view the content of an archive entry by double clicking it. This uses the default editor associated with the content type of the file inside the archive entry. You can also choose which editor to open by using the **Open With** local menu entry.

When an editor is opened this way, the file from the archive is always extracted into a temporary folder. This folder will automatically be deleted if the Zip Editor gets closed.

#### Use external editors or commands
There is also an entry **Other...** within the **Open With** sub menu which lets you call an external program to handle the extracted archive content. For instance if you want to display the content as hex dump using `xxd`, click **Open With**/**Other**/**External** and then **Add** (or **Edit** from the local menu for an existing external program). Type **xxd $p | $i** within the command text field. This means the external program `xxd` is called with the path of the extracted archive entry and the output of `xxd` is directed to the internal text editor.

You could choose to show it in another editor. Then place the cursor after the pipe symbol (|) within the command text field, delete the **$i**, press <Ctrl + Space>, choose **$e**, type <Space> and then press <Ctrl + Space> again. Now choose your favorite editor and press OK.
Sometimes an editor likes its files to have a specific file extension. Then add **$x<extension>** to the expression.

e.g. **xxd $x.hex $p | $e org.eclipse.jdt.ui.CompilationUnitEditor**

would open the file with the extension .hex

#### Modify internally opened files
Files that have been opened as described above can be modified. The Zip Editor (if it receives the focus) checks if a file that has been extracted within the temporary folder has been modified. This is done by comparing file modification times. If it detects a modification, it shows the dialog **File has been modified** which lets you choose to update the archive or not.
  
Note that you can update several files before saving the archive back to the source. This follows the normal editor life cycle. A modified archive entry is then indicated by a > in front of its name. A newly added entry btw. is indicated by a *.

#### Properties of archive entries
The **Properties** menu entry of the local sub menu lets you view and modify properties of an entry. In contrast to most other property dialogs within the Eclipse platform you can modify multiple archive entries at once. Simply select several entries, select the **Properties** menu entry, modify a value e.g. the date and press OK. All selected entries will be modified. The readonly **Size** value contains the sum of the selected entry sizes.

If one or more folder entries are selected, the **Size** reflects the sum of all entries within that folders.

### Project Explorer Integration
Zip Editor plugin extends the Project Explorer so that archive content can be browsed. The same way a file can be extracted to a temporary folder and opened within an editor can be used here. Differently to the Zip Editor, (at the moment) you cannot save back modified files.

### Archive Search
There are several ways to use the archive search feature:
#### Search within the current file
If a Zip Editor is opened, press <Ctrl + H>, choose "Selected resources", type your search text and/or the entry patterns and press "Search". The current file will be scanned for the text.
#### Search within workspace files
Either select the resources you want to search in from within a view like the Project Explorer or open the  search dialog e.g. using <Ctrl + H>. Then choose the **Zip Search** tab and select **Workspace** as the search source. This searches the entire workspace.
#### Search within libraries
Expand a library node within the Project Explorer or Package Explorer view, select the libraries you want to search in, press <Ctrl + H> to open the search dialog, choose the **Zip Search** tab and select **Selected resources**. 
#### Search within external files
Open the search dialog, choose the **Zip Search** tab and select **File system**. You can now either choose a path or files using the file system tree selector below or you type in a path with the text field above the tree selector.
#### Work with the search result view
Archive search results are displayed within the platform **Search** view. You can switch between **Show as List** and **Show as Tree** as known from text searches.
A double click on a search result which reflects an archive entry that is not itself an archive always opens the internal Text Editor with on the archive entry. Search result annotations are displayed.

The local sub menu allows to (besides the known functions)
* directly open the top level archive file using **Open archive**
* open the selected entry with the associated editor or a previously used editor
