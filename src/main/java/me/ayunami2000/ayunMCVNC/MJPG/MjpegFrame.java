package me.ayunami2000.ayunMCVNC.MJPG;

/**
 * jipCam : The Java IP Camera Project
 * Copyright (C) 2005-2006 Jason Thrasher
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

import java.awt.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.Properties;

import javax.imageio.ImageIO;


/**
 * Container for MJPEG Data Frame.
 *
 * @author Jason Thrasher
 */
public class MjpegFrame {
    private byte[] mData; //raw bytes of frame

    //	private int mHeaderLen;	//length of header data
    private int mJpegLen; //length of jpeg data
    private int mSeq; //sequence number
    private Properties mProps; //header properties

    public MjpegFrame(byte[] frame, int jpegLen, int sequence) {
        mData = frame;
        mJpegLen = jpegLen;
        mSeq = sequence;
    }

    /**
     * Create a data frame given raw JPEG data.
     *
     * @param jpegBytes the raw JPEG file bytes
     * @param sequence the frame number to bundle with this frame
     */
    public MjpegFrame(byte[] jpegBytes, int sequence) {
        mJpegLen = jpegBytes.length;
        mSeq = sequence;

        //create the MJPEG frame data
        ByteArrayOutputStream out = new ByteArrayOutputStream(jpegBytes.length +
                200);

        try {
            out.write(createHeader(mJpegLen).toString().getBytes()); //write the header
            out.write(jpegBytes); //write the jpeg data
        } catch (IOException ioe) {
            //TODO: ignore?
        }

        mData = out.toByteArray();
    }

    /**
     * Create an MJPEG header given the number of bytes in the JPEG.
     * This is similar from the header created by the Axis 2100
     *
     * @param contentLength
     * @return
     */
    public StringBuffer createHeader(int contentLength) {
        StringBuffer header = new StringBuffer(100);
        header.append(
                "\r\n\r\n--myboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ");
        header.append(contentLength);
        header.append("\r\n\r\n");

        return header;
    }

    public byte[] getJpegBytes() {
        byte[] jpeg = new byte[mJpegLen];
        System.arraycopy(mData, mData.length - mJpegLen, jpeg, 0, mJpegLen);

        return jpeg;
    }

    public byte[] getHeaderBytes() {
        byte[] header = new byte[mData.length - mJpegLen];
        System.arraycopy(mData, 0, header, 0, header.length);

        return header;
    }

    /**
     * Get this MJPEG frame with the boundary, properties, and JPEG data bytes.
     * Use this method for re-writing the frame to a stream.
     *
     * @return frame bytes
     */
    public byte[] getBytes() {
        return mData;
    }

    public int getLength() {
        return mData.length;
    }

    /**
     * Parse the MJPEG frame header for the Content-Length.
     * This value is not necessarily parsed from the MJPEG frame data
     * because the value may not exist as a header value depending on the
     * CameraAPI call that was used to get the stream.
     *
     * To retrieve the parsed value use:
     * getProperties().getProperty(MjpegFormat.CONTENT_LENGTH);
     *
     * @return the content length of the jpeg bytes, or -1 if unknown
     */
    public int getContentLength() {
        return mJpegLen;
    }

    /**
     * The "Delta-time" header may be encoded in the MJPEG stream.
     * This value is parsed from the MJPEG frame data, and may not be accurate.
     * @return
     */
    public int getDeltaTime() {
        return Integer.parseInt(getProperties().getProperty(MjpegFormat.DELTA_TIME));
    }

    public Properties getProperties() {
        if (mProps == null) {
            mProps = new Properties();

            try {
                mProps.load(new ByteArrayInputStream(mData, 0,
                        mData.length - mJpegLen));
            } catch (IOException ioe) {
                //very unlikely
                ioe.printStackTrace();
            }
        }

        return mProps;
    }

    public Image getImage() {
        InputStream is = new ByteArrayInputStream(getJpegBytes());

        try {
            return ImageIO.read(is);

            //RGBFormat rgbf = (RGBFormat)camStream.getFormat();// get the format from the stream
            //			JPEGFormat jpegF = new JPEGFormat();
            //			BufferToImage conv = new BufferToImage(jpegF); // Grab image from webcam
            //			Buffer b = new Buffer();
            //
            //			//camStream.read(b);// Convert to an AWT image
            //			b.setFormat(jpegF);
            //			b.setData(getJpegBytes());
            //
            //			Image i = conv.createImage(b);
            //
            //			return i;
        } catch (IOException ioe) {
            //unlikely
            ioe.printStackTrace();

            return null;
        }
    }

    /**
     * Get the image id - this could be the same as the count, if needed.
     * @return the ID
     */
    public int getSequence() {
        return mSeq;
    }
}