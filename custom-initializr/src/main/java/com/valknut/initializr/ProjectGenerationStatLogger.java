package com.valknut.initializr;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.spring.initializr.actuate.stat.ProjectRequestDocument;
import io.spring.initializr.actuate.stat.ProjectRequestDocumentFactory;
import io.spring.initializr.web.project.ProjectRequestEvent;

/**
 * Publish stats for each project generated to an Elastic index.
 *
 * @author Stephane Nicoll
 */
@Component
public class ProjectGenerationStatLogger
{

	private static final Log logger = LogFactory.getLog( ProjectGenerationStatLogger.class );

	private String savePathString = File.separator + "usr" + File.separator + "local" + File.separator + "inet"
			+ File.separator + "logs" + File.separator + "initializr-total-generated.txt";
	private Path savePath = Paths.get( savePathString );

	private final ProjectRequestDocumentFactory documentFactory;

	private final ObjectMapper objectMapper;

	public ProjectGenerationStatLogger() throws IOException
	{
		logger.debug( "Logging project gen stats." );
		this.documentFactory = new ProjectRequestDocumentFactory();
		this.objectMapper = createObjectMapper();
		if ( Files.notExists( savePath ) )
			Files.write(	savePath,
							"0".getBytes(),
							StandardOpenOption.CREATE );
	}

	@EventListener
	@Async
	public void handleEvent( ProjectRequestEvent event )
	{
		String json = null;
		try
		{
			ProjectRequestDocument document = this.documentFactory.createDocument( event );

			json = toJson( document );

			logger.info( "Generated: " + json );

			BigInteger count = new BigInteger( new String( Files.readAllBytes( savePath ) ) );
			logger.debug( "before: " + count );
			count = count.add( new BigInteger( "1" ) );
			logger.debug( "after: " + count );
			Files.write(	savePath,
							count	.toString()
									.getBytes(),
							StandardOpenOption.WRITE );

		}
		catch ( Exception ex )
		{
			logger.warn( "There was a problem recording metrics during project gen. JSON: " + json );
		}
	}

	private String toJson( ProjectRequestDocument stats )
	{
		try
		{
			return this.objectMapper.writeValueAsString( stats );
		}
		catch ( JsonProcessingException ex )
		{
			throw new IllegalStateException(	"Cannot convert to JSON",
												ex );
		}
	}

	private static ObjectMapper createObjectMapper()
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
		return mapper;
	}

}