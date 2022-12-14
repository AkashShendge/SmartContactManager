package com.acks.service;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
	
	public boolean sendEmail(String subject, String message, String to)
	{
		boolean f = false;
		//rest of the code...
		
		String from = "akashshendgepatil@gmail.com";
		
		//variable for gmail
				String host = "smtp.gmail.com";
				
				//get the system properties
				Properties properties = System.getProperties();
				System.out.println("Properties : "+properties);
				
				//Setting important information to properties object
				
				//host set
				properties.put("mail.smtp.host", host);
				properties.put("mail.smtp.port", "465");
				properties.put("mail.smtp.ssl.enable", "true");
				properties.put("mail.smtp.auth", "true");
				
				//step 1: to get the session object...
				Session session= Session.getInstance(properties, new Authenticator() {

					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication("akashshendgepatil@gmail.com", "Akash@1999");
					}
					
				});
				
				session.setDebug(true);
				
				//Step 2: compose the message [text, multimedia]
				MimeMessage mimeMessage = new MimeMessage(session);
				
				try {
					//from email
					mimeMessage.setFrom(from);
					
					//addinng recipient to message
					mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
					
					//adding subject to message
					mimeMessage.setSubject(subject);
					
					//adding text to message
//					mimeMessage.setText(message);
					mimeMessage.setContent(message, "text/html");
					
					//send
					
					//step 3 : send the message using Transport class
					Transport.send(mimeMessage);
					
					System.out.println("Send Success.......");
					f = true;
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				return f;
	}

}
