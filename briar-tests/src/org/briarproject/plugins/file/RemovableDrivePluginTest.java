package org.briarproject.plugins.file;

import static org.briarproject.api.transport.TransportConstants.MAX_FRAME_LENGTH;
import static org.briarproject.api.transport.TransportConstants.MIN_STREAM_LENGTH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.briarproject.BriarTestCase;
import org.briarproject.TestFileUtils;
import org.briarproject.TestUtils;
import org.briarproject.api.ContactId;
import org.briarproject.api.plugins.TransportConnectionWriter;
import org.briarproject.api.plugins.simplex.SimplexPluginCallback;
import org.briarproject.api.system.FileUtils;
import org.briarproject.plugins.ImmediateExecutor;
import org.briarproject.plugins.file.RemovableDriveMonitor.Callback;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RemovableDrivePluginTest extends BriarTestCase {

	private final File testDir = TestUtils.getTestDirectory();
	private final ContactId contactId = new ContactId(234);
	private final FileUtils fileUtils = new TestFileUtils();

	@Override
	@Before
	public void setUp() {
		testDir.mkdirs();
	}

	@Test
	public void testWriterIsNullIfNoDrivesAreFound() throws Exception {
		final List<File> drives = Collections.emptyList();

		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
			oneOf(finder).findRemovableDrives();
			will(returnValue(drives));
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);
		plugin.start();

		assertNull(plugin.createWriter(contactId));

		context.assertIsSatisfied();
	}

	@Test
	public void testWriterIsNullIfNoDriveIsChosen() throws Exception {
		final File drive1 = new File(testDir, "1");
		final File drive2 = new File(testDir, "2");
		final List<File> drives = new ArrayList<File>();
		drives.add(drive1);
		drives.add(drive2);

		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
			oneOf(finder).findRemovableDrives();
			will(returnValue(drives));
			oneOf(callback).showChoice(with(any(String[].class)),
					with(any(String.class)));
			will(returnValue(-1)); // The user cancelled the choice
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);
		plugin.start();

		assertNull(plugin.createWriter(contactId));
		File[] files = drive1.listFiles();
		assertTrue(files == null || files.length == 0);

		context.assertIsSatisfied();
	}

	@Test
	public void testWriterIsNullIfOutputDirDoesNotExist() throws Exception {
		final File drive1 = new File(testDir, "1");
		final File drive2 = new File(testDir, "2");
		final List<File> drives = new ArrayList<File>();
		drives.add(drive1);
		drives.add(drive2);

		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
			oneOf(finder).findRemovableDrives();
			will(returnValue(drives));
			oneOf(callback).showChoice(with(any(String[].class)),
					with(any(String.class)));
			will(returnValue(0)); // The user chose drive1 but it doesn't exist
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);
		plugin.start();

		assertNull(plugin.createWriter(contactId));
		File[] files = drive1.listFiles();
		assertTrue(files == null || files.length == 0);

		context.assertIsSatisfied();
	}

	@Test
	public void testWriterIsNullIfOutputDirIsAFile() throws Exception {
		final File drive1 = new File(testDir, "1");
		final File drive2 = new File(testDir, "2");
		final List<File> drives = new ArrayList<File>();
		drives.add(drive1);
		drives.add(drive2);
		// Create drive1 as a file rather than a directory
		assertTrue(drive1.createNewFile());

		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
			oneOf(finder).findRemovableDrives();
			will(returnValue(drives));
			oneOf(callback).showChoice(with(any(String[].class)),
					with(any(String.class)));
			will(returnValue(0)); // The user chose drive1 but it's not a dir
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);
		plugin.start();

		assertNull(plugin.createWriter(contactId));
		File[] files = drive1.listFiles();
		assertTrue(files == null || files.length == 0);

		context.assertIsSatisfied();
	}

	@Test
	public void testWriterIsNotNullIfOutputDirIsADir() throws Exception {
		final File drive1 = new File(testDir, "1");
		final File drive2 = new File(testDir, "2");
		final List<File> drives = new ArrayList<File>();
		drives.add(drive1);
		drives.add(drive2);
		// Create drive1 as a directory
		assertTrue(drive1.mkdir());

		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
			oneOf(finder).findRemovableDrives();
			will(returnValue(drives));
			oneOf(callback).showChoice(with(any(String[].class)),
					with(any(String.class)));
			will(returnValue(0)); // The user chose drive1
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);
		plugin.start();

		assertNotNull(plugin.createWriter(contactId));
		// The output file should exist and should be empty
		File[] files = drive1.listFiles();
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals(0, files[0].length());

		context.assertIsSatisfied();
	}

	@Test
	public void testWritingToWriter() throws Exception {
		final File drive1 = new File(testDir, "1");
		final File drive2 = new File(testDir, "2");
		final List<File> drives = new ArrayList<File>();
		drives.add(drive1);
		drives.add(drive2);
		// Create drive1 as a directory
		assertTrue(drive1.mkdir());

		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
			oneOf(finder).findRemovableDrives();
			will(returnValue(drives));
			oneOf(callback).showChoice(with(any(String[].class)),
					with(any(String.class)));
			will(returnValue(0)); // The user chose drive1
			oneOf(callback).showMessage(with(any(String.class)));
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);
		plugin.start();

		TransportConnectionWriter writer = plugin.createWriter(contactId);
		assertNotNull(writer);
		// The output file should exist and should be empty
		File[] files = drive1.listFiles();
		assertNotNull(files);
		assertEquals(1, files.length);
		assertEquals(0, files[0].length());
		// Writing to the output stream should increase the size of the file
		OutputStream out = writer.getOutputStream();
		out.write(new byte[1234]);
		out.flush();
		out.close();
		// Disposing of the writer should not delete the file
		writer.dispose(false);
		assertTrue(files[0].exists());
		assertEquals(1234, files[0].length());

		context.assertIsSatisfied();
	}

	@Test
	public void testEmptyDriveIsIgnored() throws Exception {
		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);
		plugin.start();

		plugin.driveInserted(testDir);

		context.assertIsSatisfied();
	}

	@Test
	public void testFilenames() {
		Mockery context = new Mockery();
		final Executor executor = context.mock(Executor.class);
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(executor,
				fileUtils, callback, finder, monitor, MAX_FRAME_LENGTH, 0);

		assertFalse(plugin.isPossibleConnectionFilename("abcdefg.dat"));
		assertFalse(plugin.isPossibleConnectionFilename("abcdefghi.dat"));
		assertFalse(plugin.isPossibleConnectionFilename("abcdefgh_dat"));
		assertFalse(plugin.isPossibleConnectionFilename("abcdefgh.rat"));
		assertTrue(plugin.isPossibleConnectionFilename("abcdefgh.dat"));
		assertTrue(plugin.isPossibleConnectionFilename("ABCDEFGH.DAT"));

		context.assertIsSatisfied();
	}

	@Test
	public void testReaderIsCreated() throws Exception {
		Mockery context = new Mockery();
		final SimplexPluginCallback callback =
				context.mock(SimplexPluginCallback.class);
		final RemovableDriveFinder finder =
				context.mock(RemovableDriveFinder.class);
		final RemovableDriveMonitor monitor =
				context.mock(RemovableDriveMonitor.class);

		context.checking(new Expectations() {{
			oneOf(monitor).start(with(any(Callback.class)));
			oneOf(callback).readerCreated(with(any(FileTransportReader.class)));
		}});

		RemovableDrivePlugin plugin = new RemovableDrivePlugin(
				new ImmediateExecutor(), fileUtils, callback, finder, monitor,
				MAX_FRAME_LENGTH, 0);
		plugin.start();

		File f = new File(testDir, "abcdefgh.dat");
		OutputStream out = new FileOutputStream(f);
		out.write(new byte[MIN_STREAM_LENGTH]);
		out.flush();
		out.close();
		assertEquals(MIN_STREAM_LENGTH, f.length());
		plugin.driveInserted(testDir);

		context.assertIsSatisfied();
	}

	@Override
	@After
	public void tearDown() {
		TestUtils.deleteTestDirectory(testDir);
	}
}
