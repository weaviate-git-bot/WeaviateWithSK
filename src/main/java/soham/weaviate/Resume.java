package soham.weaviate;

public class Resume {
    private String title;
    private String filepath;
    private String content;

    public Resume(String title, String filepath, String content) {
        this.title = title;
        this.filepath = filepath;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getFilepath() {
        return filepath;
    }

    public String getContent() {
        return content;
    }
}
