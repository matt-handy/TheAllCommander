package c2.http.httphandlers.telemetry;

public class TelemetryReport {

	private String timestamp;
	private String hostname;
	private String pid;
	private String measurementName;
	private String value;
	private boolean pidSpecific;
	private String type;

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getMeasurementName() {
		return measurementName;
	}

	public void setMeasurementName(String measurementName) {
		this.measurementName = measurementName;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isPidSpecific() {
		return pidSpecific;
	}

	public void setPidSpecific(boolean pidSpecific) {
		this.pidSpecific = pidSpecific;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Timestamp:" + timestamp + ", Hostname:" + hostname + ", PID:" + pid + ", Measurement:" + measurementName
				+ ", value:" + value + ",pidSpecific:" + pidSpecific + ",type:" + type;
	}
}
