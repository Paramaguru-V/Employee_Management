import java.time.LocalDateTime;

public class DownloadLog {
    private int id;
    private Manager manager;
    private LocalDateTime downloadTime;

    public DownloadLog(int id, Manager manager, LocalDateTime downloadTime) {
        this.id = id;
        this.manager = manager;
        this.downloadTime = downloadTime;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public LocalDateTime getDownloadTime() {
        return downloadTime;
    }

    public void setDownloadTime(LocalDateTime downloadTime) {
        this.downloadTime = downloadTime;
    }
}
