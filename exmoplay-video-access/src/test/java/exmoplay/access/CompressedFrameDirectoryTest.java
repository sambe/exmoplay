/*
 * Copyright (c) 2013 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Feb 19, 2013
 */
package exmoplay.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class CompressedFrameDirectoryTest {

    @Test
    public void testDiffUndiff() {
        int[] list = { 1, 2, 3, 4, 6, 7, 9, 6, 4, 2, 0, -2 };
        int[] expectedDiffs = { 1, 1, 1, 1, 2, 1, 2, -3, -2, -2, -2, -2 };
        int[] diffs = CompressedFrameDirectory.diff(list);
        arrayEquals(expectedDiffs, diffs);

        int[] undiffed = CompressedFrameDirectory.undiff(diffs);
        arrayEquals(list, undiffed);
    }

    @Test
    public void testLongDiffUndiff() {
        long[] list = { 1, 2, 3, 4, 6, 7, 9, 6, 4, 2, 0, -2 };
        int[] expectedDiffs = { 1, 1, 1, 1, 2, 1, 2, -3, -2, -2, -2, -2 };
        int[] diffs = CompressedFrameDirectory.longDiff(list);
        arrayEquals(expectedDiffs, diffs);

        long[] undiffed = CompressedFrameDirectory.longUndiff(diffs);
        arrayEquals(list, undiffed);
    }

    @Test
    public void testCompressUncompress() {
        int[] list = { 5, 5, 5, 5, 6, 6, 4, 4, 4, 2, 2, 7, 9, 1, 1, 1 };
        int[] expectedCompressed = { list.length, 5, 4, 6, 6, 4, 9, 2, 11, 7, 12, 9, 13, 1 };

        int[] compressed = CompressedFrameDirectory.compress(list);
        arrayEquals(expectedCompressed, compressed);

        int[] uncompressed = CompressedFrameDirectory.uncompress(compressed);
        arrayEquals(list, uncompressed);
    }

    @Test
    public void testCompressionFrameDirectoryVideo() {
        List<MediaInfo.VideoPictureInfo> l = new ArrayList<MediaInfo.VideoPictureInfo>();
        for (int i = 0; i < 200; i++) {
            long timestamp = (i + 1) * 16 - 3;
            l.add(new MediaInfo.VideoPictureInfo(i, timestamp, i % 30 == 0));
        }

        CompressedFrameDirectory.Video cv = CompressedFrameDirectory.compressVideo(l);

        List<MediaInfo.VideoPictureInfo> ul = cv.decompress();
        Assert.assertEquals("Size of returned list differs from size of original list", l.size(), ul.size());
        for (int i = 0; i < l.size(); i++) {
            MediaInfo.VideoPictureInfo exp = l.get(i);
            MediaInfo.VideoPictureInfo act = ul.get(i);
            Assert.assertEquals("nr different on element " + i, exp.nr, act.nr);
            Assert.assertEquals("timestamp different on element " + i, exp.timestamp, act.timestamp);
            Assert.assertEquals("key different on element " + i, exp.key, act.key);
        }
    }

    @Test
    public void testCompressionFrameDirectoryAudio() {
        List<MediaInfo.AudioSamplesInfo> l = new ArrayList<MediaInfo.AudioSamplesInfo>();
        for (int i = 0; i < 200; i++) {
            long timestamp = (i + 1) * 16 - 3;
            long offset = i * 65536;
            int length = 65536;
            l.add(new MediaInfo.AudioSamplesInfo(i, timestamp, offset, length));
        }

        CompressedFrameDirectory.Audio ca = CompressedFrameDirectory.compressAudio(l);

        List<MediaInfo.AudioSamplesInfo> ul = ca.decompress();
        Assert.assertEquals("Size of returned list differs from size of original list", l.size(), ul.size());
        for (int i = 0; i < l.size(); i++) {
            MediaInfo.AudioSamplesInfo exp = l.get(i);
            MediaInfo.AudioSamplesInfo act = ul.get(i);
            Assert.assertEquals("nr different on element " + i, exp.nr, act.nr);
            Assert.assertEquals("timestamp different on element " + i, exp.timestamp, act.timestamp);
            Assert.assertEquals("samplesOffset different on element " + i, exp.samplesOffset, act.samplesOffset);
            Assert.assertEquals("samplesLength different on element " + i, exp.samplesLength, act.samplesLength);
        }
    }

    private void arrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Arrays are different:\n");
            builder.append("expected: " + Arrays.toString(expected) + "\n");
            builder.append("actual  : " + Arrays.toString(actual) + "\n");
            Assert.fail(builder.toString());
        }
    }

    private void arrayEquals(long[] expected, long[] actual) {
        Assert.assertTrue(Arrays.equals(expected, actual));
    }
}
