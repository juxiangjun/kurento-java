/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

package org.kurento.test.functional.datachannels;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.test.base.FunctionalTest;
import org.kurento.test.browser.WebRtcChannel;
import org.kurento.test.browser.WebRtcMode;
import org.kurento.test.config.TestScenario;

/**
 * Test Datachannel Back2Back Browser - WebRTC - WebRTC - Browser </ul> Browser(s):
 * <ul>
 * ·CHROME CHROME <br>
 * ·CHROME FIREFOX <br>
 * ·CHROME FIREFOX-BETA <br>
 * ·CHROME-BETA CHROME-BETA <br>
 * ·CHROME-BETA FIREFOX <br>
 * ·CHROME-BETA FIREFOX-BETA <br>
 * ·CHROME-DEV CHROME-DEV <br>
 * ·CHROME-DEV FIREFOX <br>
 * ·CHROME-DEV FIREFOX-BETA <br>
 * ·FIREFOX FIREFOX <br>
 * ·FIREFOX CHROME <br>
 * ·FIREFOX CHROME-BETA <br>
 * ·FIREFOX CHROME-DEV <br>
 * ·FIREFOX-BETA FIREFOX-BETA <br>
 * ·FIREFOX-BETA CHROME <br>
 * ·FIREFOX-BETA CHROME-BETA <br>
 * ·FIREFOX-BETA CHROME-DEV <br>
 * </ul>
 * Test logic:
 * <ol>
 * <li>Start two browsers with data channel</li>
 * <li>Send 50 messages between both browsers</li>
 * </ol>
 * Main assertion(s): <br>
 * All messages must be right in both browsers
 *
 * @author Raul Benitez (rbenitez@gsyc.es)
 * @since 6.4.1
 */
public class DatachannelsB2BTest extends FunctionalTest {

  private Integer TIMES = 50;

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return TestScenario.localChromesAndFirefoxs(2);
  }

  @Test
  public void testDispatcherPlayer() throws Exception {
    // Media Pipeline
    MediaPipeline mp = kurentoClient.createMediaPipeline();

    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(mp).useDataChannels().build();
    WebRtcEndpoint webRtcEp2 = new WebRtcEndpoint.Builder(mp).useDataChannels().build();

    webRtcEp.connect(webRtcEp2);
    webRtcEp2.connect(webRtcEp);

    // Test execution
    getPage(0).initWebRtc(webRtcEp, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY, true);
    getPage(1).initWebRtc(webRtcEp2, WebRtcChannel.AUDIO_AND_VIDEO, WebRtcMode.RCV_ONLY, true);

    Thread.sleep(2000);

    for (int i = 0; i < TIMES; i++) {
      String messageSentBrower0 = "Data sent from the browser0. Message" + i;
      String messageSentBrower1 = "Data sent from the browser1. Message" + i;

      getPage(0).sendDataByDataChannel(messageSentBrower0);
      getPage(1).sendDataByDataChannel(messageSentBrower1);

      Assert.assertTrue("The message should be: " + messageSentBrower1, getPage(0)
          .compareDataChannelMessage(messageSentBrower1));

      Assert.assertTrue("The message should be: " + messageSentBrower0, getPage(1)
          .compareDataChannelMessage(messageSentBrower0));
    }

    // Release Media Pipeline
    mp.release();
  }
}
