package dk.digitalidentity.os2vikar.dao.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "websocket_queue")
@Getter
@Setter
public class WebsocketQueue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private LocalDateTime tts;

	@Column
	private String command;

	@Column
	private String target;

	@Column
	private String payload;
}
