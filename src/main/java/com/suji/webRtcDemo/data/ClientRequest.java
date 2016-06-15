package com.suji.webRtcDemo.data;



public class ClientRequest {
	
    private String command; // connect, sendMessage, listUsers 
    private String fromUserId;
    private String toUserId;
    private String message;
    
    public ClientRequest() {
    }
    
    public ClientRequest(String command, String fromUserId, String toUserId, String message) {
    	this.command = command;
    	this.fromUserId = fromUserId;
    	this.toUserId = toUserId;
    	this.message = message;
    }

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getFromUserId() {
		return fromUserId;
	}

	public void setFromUserId(String fromUserId) {
		this.fromUserId = fromUserId;
	}

	public String getToUserId() {
		return toUserId;
	}

	public void setToUserId(String toUserId) {
		this.toUserId = toUserId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result
				+ ((fromUserId == null) ? 0 : fromUserId.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result
				+ ((toUserId == null) ? 0 : toUserId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClientRequest other = (ClientRequest) obj;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (fromUserId == null) {
			if (other.fromUserId != null)
				return false;
		} else if (!fromUserId.equals(other.fromUserId))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (toUserId == null) {
			if (other.toUserId != null)
				return false;
		} else if (!toUserId.equals(other.toUserId))
			return false;
		return true;
	}
	
	    
}

