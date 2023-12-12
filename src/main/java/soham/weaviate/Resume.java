package soham.weaviate;

public class Resume {
    private String title;
    private String filepath;
    private String contentVector;

    public Resume(String title, String filepath, String contentVector) {
        this.title = title;
        this.filepath = filepath;
        this.contentVector = contentVector;
    }

    public String getTitle() {
        return title;
    }

    public String getFilepath() {
        return filepath;
    }

    public String getContentVector() {
        return contentVector;
    }
}
