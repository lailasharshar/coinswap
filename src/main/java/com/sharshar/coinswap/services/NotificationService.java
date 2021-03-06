package com.sharshar.coinswap.services;

import com.sendgrid.*;
import com.sharshar.coinswap.utils.ScratchException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;


/**
 * Used to send an email using SendGrid - Currently, only one person can be notified
 *
 * Created by lsharshar on 3/18/2018.
 */
@Service
public class NotificationService {
	private static Logger logger = LogManager.getLogger();

	@Value( "${sendgrid.api_key}" )
	private String apiKey;

	@Value( "${notification.sendFrom}" )
	private String sendFrom;

	@Value( "${notification.sendTo}" )
	private String sendTo;

	@Value( "${notification.smsAddress}" )
	private String smsAddress;

	public void notifyMe(String subject, String contentString) throws ScratchException {
		Email from = new Email(sendFrom);
		Email to = new Email(sendTo);
		Content content = new Content("text/html", contentString);
		Mail mail = new Mail(from, subject, to, content);
		processMsg(mail);
	}

	public void textMe(String subject, String val) throws ScratchException {
		Email from = new Email(sendFrom);
		Email to = new Email(smsAddress);
		Content content = new Content("text/plain", val);
		logger.info("Texting: " + subject + ", " + val);
		Mail mail = new Mail(from, subject, to, content);
		processMsg(mail);
	}

	private void processMsg(Mail mail) throws ScratchException {

		SendGrid sg = new SendGrid(apiKey);
		Request request = new Request();
		Response response = null;
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			response = sg.api(request);
		} catch (IOException ex) {
			logger.error(response.getStatusCode());
			logger.error(response.getBody());
			logger.error(response.getHeaders());
			if (mail.getContent() != null && mail.getContent().size() > 0) {
				logger.error(mail.getSubject() + " - " + mail.getContent().get(0).getValue());
			}
			throw new ScratchException("Error sending notification to me", ex);
		}
	}
}
