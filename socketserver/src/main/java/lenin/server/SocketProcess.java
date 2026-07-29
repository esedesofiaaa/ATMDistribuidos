package lenin.server;

import java.util.List;

public interface SocketProcess {
  public boolean bind();
  public List<Object> listen();
  public boolean response(List<Object> data);
  public boolean close();
}         
