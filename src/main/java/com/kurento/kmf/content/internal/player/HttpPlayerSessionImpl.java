/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.content.internal.player;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.common.exception.Assert;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.content.ContentCommand;
import com.kurento.kmf.content.ContentCommandResult;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpPlayerSession;
import com.kurento.kmf.content.internal.ContentSessionManager;
import com.kurento.kmf.content.internal.base.AbstractHttpBasedContentSession;
import com.kurento.kmf.media.HttpEndpoint;
import com.kurento.kmf.media.HttpGetEndpoint.HttpGetEndpointBuilder;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlayerEndpoint;
import com.kurento.kmf.media.UriEndpoint;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.MediaSessionTerminatedEvent;
import com.kurento.kmf.repository.RepositoryHttpEndpoint;
import com.kurento.kmf.repository.RepositoryItem;

/**
 * 
 * Request implementation for a Player.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class HttpPlayerSessionImpl extends AbstractHttpBasedContentSession
		implements HttpPlayerSession {

	private static final Logger log = LoggerFactory
			.getLogger(HttpPlayerSessionImpl.class);

	private final boolean terminateOnEOS;

	public HttpPlayerSessionImpl(HttpPlayerHandler handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol,
			boolean terminateOnEOS) {
		super(handler, manager, asyncContext, contentId, redirect,
				useControlProtocol);
		this.terminateOnEOS = terminateOnEOS;
	}

	@Override
	protected HttpPlayerHandler getHandler() {
		return (HttpPlayerHandler) super.getHandler();
	}

	@Override
	public void start(String contentPath) {
		try {
			Assert.notNull(contentPath, "Illegal null contentPath provided",
					10027);
			activateMedia(contentPath, (MediaElement[]) null);
		} catch (KurentoMediaFrameworkException ke) {
			internalTerminateWithError(null, ke.getCode(), ke.getMessage(),
					null);
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20029);
			internalTerminateWithError(null, kmfe.getCode(), kmfe.getMessage(),
					null);
			throw kmfe;
		}
	}

	/**
	 * Perform a play action using a MediaElement.
	 */
	@Override
	public void start(MediaElement element) {
		try {
			Assert.notNull(element, "Illegal null source element provided",
					10028);
			activateMedia(null, new MediaElement[] { element });

		} catch (KurentoMediaFrameworkException ke) {
			internalTerminateWithError(null, ke.getCode(), ke.getMessage(),
					null);
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20029);
			internalTerminateWithError(null, kmfe.getCode(), kmfe.getMessage(),
					null);
			throw kmfe;
		}
	}

	@Override
	protected void activateMedia(RepositoryItem repositoryItem) {
		super.activateMedia(repositoryItem);
		addMediaSessionTerminatedListener();
	}

	@Override
	protected void activateMedia(String contentPath,
			MediaElement... mediaElements) {
		super.activateMedia(contentPath, mediaElements);
		addMediaSessionTerminatedListener();
	}

	private void addMediaSessionTerminatedListener() {
		httpEndpoint
				.addMediaSessionTerminatedListener(new MediaEventListener<MediaSessionTerminatedEvent>() {
					@Override
					public void onEvent(MediaSessionTerminatedEvent event) {
						getLogger().info(
								"Received event with type " + event.getType());
						internalTerminateWithoutError(null, 1,
								"MediaServer MediaSessionTerminated", null); // TODO

					}
				});
	}

	@Override
	public void start(RepositoryItem repositoryItem) {
		try {
			Assert.notNull(repositoryItem, "Illegal null repository provided",
					10027);
			activateMedia(repositoryItem);
		} catch (KurentoMediaFrameworkException ke) {
			internalTerminateWithError(null, ke.getCode(), ke.getMessage(),
					null);
			throw ke;
		} catch (Throwable t) {
			KurentoMediaFrameworkException kmfe = new KurentoMediaFrameworkException(
					t.getMessage(), t, 20029);
			internalTerminateWithError(null, kmfe.getCode(), kmfe.getMessage(),
					null);
			throw kmfe;
		}
	}

	@Override
	protected UriEndpoint buildUriEndpoint(String contentPath) {
		getLogger().info("Creating media pipeline ...");
		MediaPipeline mediaPipeline = mediaPipelineFactory.create();
		releaseOnTerminate(mediaPipeline);
		getLogger().info("Creating PlayerEndpoint ...");
		PlayerEndpoint playerEndpoint = mediaPipeline.newPlayerEndpoint(
				contentPath).build();
		return playerEndpoint;
	}

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
	@Override
	protected HttpEndpoint buildAndConnectHttpEndpoint(
			MediaElement... mediaElements) {

		// In this case (player) we can connect to one media element
		// (source) that must be the first in the array. This is not very
		// beautiful but makes possible to have player and recorder on the
		// same inheritance hierarchy
		MediaElement mediaElement = mediaElements[0];
		getLogger().info("Recovering media pipeline");
		MediaPipeline mediaPiplePipeline = mediaElement.getMediaPipeline();
		getLogger().info("Creating HttpEndpoint ...");
		HttpGetEndpointBuilder builder = mediaPiplePipeline
				.newHttpGetEndpoint();

		if (terminateOnEOS) {
			builder.terminateOnEOS();
		}

		HttpEndpoint httpEndpoint = builder.build();

		releaseOnTerminate(httpEndpoint);
		mediaElement.connect(httpEndpoint);

		getLogger().info(
				"Adding PlayerEndpoint.play() into HttpEndpoint listener");

		return httpEndpoint;
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void interalRawCallToOnSessionTerminated(int code,
			String description) throws Exception {
		getHandler().onSessionTerminated(this, code, description);
	}

	@Override
	protected void interalRawCallToOnContentStarted() throws Exception {
		getHandler().onContentStarted(this);
	}

	@Override
	protected void interalRawCallToOnContentError(int code, String description)
			throws Exception {
		getHandler().onSessionError(this, code, description);
	}

	@Override
	protected void internalRawCallToOnContentRequest() throws Exception {
		getHandler().onContentRequest(this);
	}

	@Override
	protected void internalRawCallToOnUncaughtExceptionThrown(Throwable t)
			throws Exception {
		getHandler().onUncaughtException(this, t);

	}

	@Override
	protected ContentCommandResult interalRawCallToOnContentCommand(
			ContentCommand command) throws Exception {
		return getHandler().onContentCommand(this, command);
	}

	@Override
	protected RepositoryHttpEndpoint createRepositoryHttpEndpoint(
			RepositoryItem repositoryItem) {
		return repositoryItem.createRepositoryHttpPlayer();
		// TODO: who releases this?
		// Should it be released in a per-session basis? In that case, we cannot
		// re-use if useControlProtocol = false.
	}

}
