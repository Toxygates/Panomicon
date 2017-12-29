package t.common.client;

public class RunCommand {

  private Runnable action;
  private String title;
  
  public RunCommand(String title, Runnable action) {
    this.title = title;
    this.action = action;
  }

  public String getTitle() {
    return title;
  }
  
  public void run() {
    action.run();
  }
}
