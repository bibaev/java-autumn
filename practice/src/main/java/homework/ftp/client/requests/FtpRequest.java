package homework.ftp.client.requests;

import homework.ftp.client.ex.RequestException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;

public abstract class FtpRequest<R> implements Request<R> {
  @NotNull
  @Override
  public R execute(@NotNull Socket socket) throws RequestException {
    try {
      return executeImpl(socket);
    } catch (IOException e) {
      throw new RequestException(e);
    }
  }

  protected abstract R executeImpl(@NotNull Socket socket) throws IOException, RequestException;
}
