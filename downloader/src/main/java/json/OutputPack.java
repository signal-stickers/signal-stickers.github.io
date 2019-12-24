package json;

import java.util.List;

public class OutputPack {
  public String       title;
  public String       author;
  public List<String> tags;
  public String       url;
  public String       coverFileName;

  public OutputPack(String title, String author, List<String> tags, String url, String coverFileName) {
    this.title = title;
    this.author = author;
    this.tags = tags;
    this.url = url;
    this.coverFileName = coverFileName;
  }
}
