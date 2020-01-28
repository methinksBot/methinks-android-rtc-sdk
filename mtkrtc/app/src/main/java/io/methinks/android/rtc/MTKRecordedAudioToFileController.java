/*
 *  Copyright 2018 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package io.methinks.android.rtc;

import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioRecord.WebRtcAudioRecordSamplesReadyCallback;

/**
 * Implements the AudioRecordSamplesReadyCallback interface and writes
 * recorded raw audio samples to an output file.
 */
public class MTKRecordedAudioToFileController implements SamplesReadyCallback, WebRtcAudioRecordSamplesReadyCallback {
    private static final String TAG = "RecordedAudioToFile";
//    private static final long MAX_FILE_SIZE_IN_BYTES = 58348800L;
//
//    private final Object lock = new Object();
//    private final ExecutorService executor;
//    @Nullable
//    private OutputStream rawAudioFileOutputStream = null;
//    private boolean isRunning;
//    private long fileSizeInBytes = 0;
//
//    public MTKRecordedAudioToFileController(ExecutorService executor) {
//        Log.d(TAG, "ctor");
//        this.executor = executor;
//    }
//
//    /**
//     * Should be called on the same executor thread as the one provided at
//     * construction.
//     */
//    public boolean start() {
//        Log.d(TAG, "start");
//        if (!isExternalStorageWritable()) {
//            Log.e(TAG, "Writing to external media is not possible");
//            return false;
//        }
//        synchronized (lock) {
//            isRunning = true;
//        }
//
//        startTranscription();
//        return true;
//    }
//
//    /**
//     * Should be called on the same executor thread as the one provided at
//     * construction.
//     */
//    public void stop() {
//        Log.d(TAG, "stop");
//        stopTranscription();
//        synchronized (lock) {
//            isRunning = false;
//            if (rawAudioFileOutputStream != null) {
//                try {
//                    rawAudioFileOutputStream.close();
//                } catch (IOException e) {
//                    Log.e(TAG, "Failed to close file with saved input audio: " + e);
//                }
//                rawAudioFileOutputStream = null;
//            }
//            fileSizeInBytes = 0;
//        }
//    }
//
//    // Checks if external storage is available for read and write.
//    private boolean isExternalStorageWritable() {
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED.equals(state)) {
//            return true;
//        }
//        return false;
//    }
//
//    // Utilizes audio parameters to create a file name which contains sufficient
//    // information so that the file can be played using an external file player.
//    // Example: /sdcard/recorded_audio_16bits_48000Hz_mono.pcm.
//    private void openRawAudioOutputFile(int sampleRate, int channelCount) {
//        final String fileName = Environment.getExternalStorageDirectory().getPath() + File.separator
//                + "recorded_audio_16bits_" + String.valueOf(sampleRate) + "Hz"
//                + ((channelCount == 1) ? "_mono" : "_stereo") + ".pcm";
//        final File outputFile = new File(fileName);
//        try {
//            rawAudioFileOutputStream = new FileOutputStream(outputFile);
//        } catch (FileNotFoundException e) {
//            Log.e(TAG, "Failed to open audio output file: " + e.getMessage());
//        }
//        Log.d(TAG, "Opened file for recording: " + fileName);
//    }
//
//    // Called when new audio samples are ready.
//    @Override
    public void onWebRtcAudioRecordSamplesReady(WebRtcAudioRecord.AudioSamples samples) {
        Log.d("legacy onWebRtcAudioRecordSamplesReady() called!");
        onWebRtcAudioRecordSamplesReady(new JavaAudioDeviceModule.AudioSamples(samples.getAudioFormat(),
                samples.getChannelCount(), samples.getSampleRate(), samples.getData()));
    }
//
//
//    private byte[] mBuffer;
//    private long mLastVoiceHeardMillis = Long.MAX_VALUE;
//    private long mVoiceStartedMillis;
//    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
//    private static final int MAX_SPEECH_LENGTH_MILLIS = 30 * 1000;
//    // Called when new audio samples are ready.
//    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples samples) {
////        Log.e(TAG, "java onWebRtcAudioRecordSamplesReady() called!");
//        // The native audio layer on Android should use 16-bit PCM format.
//        if (samples.getAudioFormat() != AudioFormat.ENCODING_PCM_16BIT) {
//            Log.e(TAG, "Invalid audio format");
//            return;
//        }
//        synchronized (lock) {
//            // Abort early if stop() has been called.
//            if (!isRunning) {
//                return;
//            }
//            // Open a new file for the first callback only since it allows us to add audio parameters to
//            // the file name.
//            if (rawAudioFileOutputStream == null) {
//                fileSizeInBytes = 0;
//            }
//        }
//        // Append the recorded 16-bit audio samples to the open output file.
//        executor.execute(() -> {
//            // for transcription
//            ////////////////////////////////////////////////////////////////////
//            if (fileSizeInBytes < MAX_FILE_SIZE_IN_BYTES) {
//                if(isStartedTranscription && MTKDataStore.getInstance().mainPublisher != null && MTKDataStore.getInstance().mainPublisher.isPublishAudio()){
//                    mBuffer = Arrays.copyOf(samples.getData(), samples.getData().length);
//                    if(isStartedTranscription && mSpeechService != null && !mSpeechService.isStarting()){
//                        Log.e("speech", "222222222222222");
//                        mSpeechService.startRecognizing(samples.getSampleRate(), MTKDataStore.getInstance().client.lastSttLang);
//                    }
//
//                    if(isStartedTranscription && mSpeechService != null && mSpeechService.isStarting()){
//                        mSpeechService.recognize(mBuffer, mBuffer.length);
//                    }
//
//
//
////                    final long now = System.currentTimeMillis();
////                    if (isHearingVoice(mBuffer, mBuffer.length)) {
////                        Log.e(TAG, "speech isHearingVoice");
////                        if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
////                            Log.e("speech", "111111111111111111");
////                            mVoiceStartedMillis = now;
////                            if(isStartedTranscription && mSpeechService != null){
////                                Log.e("speech", "222222222222222");
////                                mSpeechService.startRecognizing(samples.getSampleRate(), MTKDataStore.getInstance().client.lastSttLang);
////                            }
////                        }
////                        if(isStartedTranscription && mSpeechService != null){
////                            mSpeechService.recognize(mBuffer, mBuffer.length);
////                        }
////                        mLastVoiceHeardMillis = now;
////                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
////                            mLastVoiceHeardMillis = Long.MAX_VALUE;
////                            if(isStartedTranscription && mSpeechService != null){
////                                mSpeechService.finishRecognizing();
////                            }
////                        }
////                    } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
////                        if(isStartedTranscription && mSpeechService != null){
////                            mSpeechService.recognize(mBuffer, mBuffer.length);
////                        }
////                        if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
////                            mLastVoiceHeardMillis = Long.MAX_VALUE;
////                            if(isStartedTranscription && mSpeechService != null){
////                                mSpeechService.finishRecognizing();
////                            }
////                        }
////                    }
////                    if (isHearingVoice(mBuffer, mBuffer.length)) {
////                        Log.e(TAG, "speech isHearingVoice");
////                        if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
////                            Log.e("speech", "111111111111111111");
////                            mVoiceStartedMillis = now;
////                            if(isStartedTranscription && mSpeechService != null){
////                                Log.e("speech", "222222222222222");
////                                mSpeechService.startRecognizing(samples.getSampleRate(), MTKDataStore.getInstance().client.lastSttLang);
////                            }
////                        }
////                        if(isStartedTranscription && mSpeechService != null){
////                            mSpeechService.recognize(mBuffer, mBuffer.length);
////                        }
////                        mLastVoiceHeardMillis = now;
////                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
////                            mLastVoiceHeardMillis = Long.MAX_VALUE;
////                            if(isStartedTranscription && mSpeechService != null){
////                                mSpeechService.finishRecognizing();
////                            }
////                        }
////                    } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
////                        if(isStartedTranscription && mSpeechService != null){
////                            mSpeechService.recognize(mBuffer, mBuffer.length);
////                        }
////                        if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
////                            mLastVoiceHeardMillis = Long.MAX_VALUE;
////                            if(isStartedTranscription && mSpeechService != null){
////                                mSpeechService.finishRecognizing();
////                            }
////                        }
////                    }
//                }else{
//                    mBuffer = null;
//                }
//            }
//            ///////////////////////////////////////////////////////////
//
//
//
//
//            if (rawAudioFileOutputStream != null) {
//                try {
//                    // Set a limit on max file size. 58348800 bytes corresponds to
//                    // approximately 10 minutes of recording in mono at 48kHz.
//                    if (fileSizeInBytes < MAX_FILE_SIZE_IN_BYTES) {
//                        // Writes samples.getData().length bytes to output stream.
//                        rawAudioFileOutputStream.write(samples.getData());
//                        fileSizeInBytes += samples.getData().length;
//                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "Failed to write audio to file: " + e.getMessage());
//                }
//            }
//        });
//    }
//
//    private boolean isStartedTranscription = false;
//    private SpeechService mSpeechService; // for Transcription.
//    private ServiceConnection mServiceConnection;
//    private SpeechService.Listener mSpeechServiceListener;
//    private void initTranscription() {
//        mSpeechServiceListener =
//                (text, isFinal) -> {
//                    Log.e(TAG, "speech onSpeechRecognized() in CustomAudioDevice");
//                    if (isFinal) {
//                        if (!TextUtils.isEmpty(text)) {
//                            MTKDataStore.getInstance().client.listener.onResultTranscription(text);
//                            Log.e(TAG, "speech onSpeechRecognized() text : " + text);
////                            callback.getText(text);
//                        }
//
//                    }
//                };
//
//
//        mServiceConnection = new ServiceConnection() {
//            @Override
//            public void onServiceConnected(ComponentName componentName, IBinder binder) {
//                Log.e(TAG, "speech onServiceConnected()");
//                mSpeechService = SpeechService.from(binder);
//                mSpeechService.addListener(mSpeechServiceListener);
//            }
//
//            @Override
//            public void onServiceDisconnected(ComponentName componentName) {
//                Log.e(TAG, "speech onServiceDisconnected()");
//            }
//        };
//    }
//
//    private void startTranscription(){
//        Log.e(TAG, "startTranscription()");
//        initTranscription();
//        isStartedTranscription = true;
//        MTKDataStore.getInstance().context.bindService(new Intent(MTKDataStore.getInstance().context, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
//    }
//
//    private void stopTranscription(){
//        isStartedTranscription = false;
//        if(mSpeechService != null){
//            mSpeechService = null;
//        }
//        if(mServiceConnection != null){
//            MTKDataStore.getInstance().context.unbindService(mServiceConnection);
//            mServiceConnection = null;
//        }
//    }
//
//    private static final int AMPLITUDE_THRESHOLD = 1500;
//    private boolean isHearingVoice(byte[] buffer, int size) {
//        for (int i = 0; i < size - 1; i += 2) {
//            // The buffer has LINEAR16 in little endian.
//            int s = buffer[i + 1];
//            if (s < 0) s *= -1;
//            s <<= 8;
//            s += Math.abs(buffer[i]);
//            if (s > AMPLITUDE_THRESHOLD) {
//                return true;
//            }
//        }
//        return false;
    }
}
