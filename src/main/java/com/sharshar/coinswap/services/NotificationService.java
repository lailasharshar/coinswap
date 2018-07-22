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

	public void notifyMe(String subject, String contentString) throws ScratchException {
		Email from = new Email(sendFrom);
		Email to = new Email(sendTo);
		Content content = new Content("text/html", contentString);
		Mail mail = new Mail(from, subject, to, content);

		SendGrid sg = new SendGrid(apiKey);
		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			Response response = sg.api(request);
			logger.info(response.getStatusCode());
			logger.info(response.getBody());
			logger.info(response.getHeaders());
		} catch (IOException ex) {
			throw new ScratchException("Error sending notification to me", ex);
		}
	}
}
