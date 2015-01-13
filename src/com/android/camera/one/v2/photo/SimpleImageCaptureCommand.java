/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.one.v2.photo;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;

import com.android.camera.async.BufferQueue;
import com.android.camera.async.Updatable;
import com.android.camera.one.v2.camera2proxy.CameraCaptureSessionClosedException;
import com.android.camera.one.v2.camera2proxy.ImageProxy;
import com.android.camera.one.v2.core.FrameServer;
import com.android.camera.one.v2.core.RequestBuilder;
import com.android.camera.one.v2.core.ResourceAcquisitionFailedException;
import com.android.camera.one.v2.imagesaver.ImageSaver;
import com.android.camera.one.v2.sharedimagereader.ImageStreamFactory;
import com.android.camera.one.v2.sharedimagereader.imagedistributor.ImageStream;

import java.util.Arrays;

/**
 * Captures single images.
 */
class SimpleImageCaptureCommand implements ImageCaptureCommand {
    private final FrameServer mFrameServer;
    private final RequestBuilder.Factory mBuilderFactory;
    private final ImageStreamFactory mImageReader;

    public SimpleImageCaptureCommand(FrameServer frameServer, RequestBuilder.Factory builder,
            ImageStreamFactory imageReader) {
        mFrameServer = frameServer;
        mBuilderFactory = builder;
        mImageReader = imageReader;
    }

    /**
     * Sends a request to take a picture and blocks until it completes.
     */
    public void run(Updatable<Void> imageExposureUpdatable, ImageSaver imageSaver) throws
            InterruptedException, CameraAccessException, CameraCaptureSessionClosedException,
            ResourceAcquisitionFailedException {
        try (FrameServer.Session session = mFrameServer.createSession();
                ImageStream imageStream = mImageReader.createStream(1)) {
            RequestBuilder photoRequest = mBuilderFactory.create(CameraDevice
                    .TEMPLATE_STILL_CAPTURE);
            photoRequest.addStream(imageStream);
            photoRequest.addResponseListener(new FrameExposureResponseListener(
                    imageExposureUpdatable));
            session.submitRequest(Arrays.asList(photoRequest.build()),
                    FrameServer.RequestType.NON_REPEATING);

            ImageProxy image = imageStream.getNext();
            imageSaver.addFullSizeImage(image);
        } catch (BufferQueue.BufferQueueClosedException e) {
            // If we get here, the request was submitted, but the image
            // never arrived.
            // TODO Log failure and notify the caller
        } finally {
            imageSaver.close();
        }
    }
}
