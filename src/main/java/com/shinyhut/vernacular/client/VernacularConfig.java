//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.shinyhut.vernacular.client;

import com.shinyhut.vernacular.client.exceptions.VncException;
import com.shinyhut.vernacular.client.rendering.ColorDepth;
import java.awt.Image;
import java.awt.Point;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VernacularConfig {
	private Supplier<String> usernameSupplier;
	private Supplier<String> passwordSupplier;
	private Consumer<VncException> errorListener;
	private Consumer<Image> screenUpdateListener;
	private Consumer<Void> bellListener;
	private Consumer<String> remoteClipboardListener;
	private Consumer<byte[]> qemuAudioListener;
	private BiConsumer<Image, Point> mousePointerUpdateListener;
	private boolean shared = true;
	private int targetFramesPerSecond = 30;
	private ColorDepth colorDepth;
	private boolean useLocalMousePointer;
	private boolean enableCopyrectEncoding;
	private boolean enableRreEncoding;
	private boolean enableHextileEncoding;
	private boolean enableZLibEncoding;
	private boolean enableQemuAudioEncoding;

	public VernacularConfig() {
		this.colorDepth = ColorDepth.BPP_8_INDEXED;
		this.useLocalMousePointer = false;
		this.enableCopyrectEncoding = true;
		this.enableRreEncoding = true;
		this.enableHextileEncoding = true;
		this.enableZLibEncoding = false;
		this.enableQemuAudioEncoding = false;
	}

	public Supplier<String> getUsernameSupplier() {
		return this.usernameSupplier;
	}

	public void setUsernameSupplier(Supplier<String> usernameSupplier) {
		this.usernameSupplier = usernameSupplier;
	}

	public Supplier<String> getPasswordSupplier() {
		return this.passwordSupplier;
	}

	public void setPasswordSupplier(Supplier<String> passwordSupplier) {
		this.passwordSupplier = passwordSupplier;
	}

	public Consumer<VncException> getErrorListener() {
		return this.errorListener;
	}

	public void setErrorListener(Consumer<VncException> errorListener) {
		this.errorListener = errorListener;
	}

	public Consumer<Image> getScreenUpdateListener() {
		return this.screenUpdateListener;
	}

	public void setScreenUpdateListener(Consumer<Image> screenUpdateListener) {
		this.screenUpdateListener = screenUpdateListener;
	}

	public BiConsumer<Image, Point> getMousePointerUpdateListener() {
		return this.mousePointerUpdateListener;
	}

	public void setMousePointerUpdateListener(BiConsumer<Image, Point> mousePointerUpdateListener) {
		this.mousePointerUpdateListener = mousePointerUpdateListener;
	}

	public Consumer<String> getRemoteClipboardListener() {
		return this.remoteClipboardListener;
	}

	public void setRemoteClipboardListener(Consumer<String> remoteClipboardListener) {
		this.remoteClipboardListener = remoteClipboardListener;
	}

	public Consumer<byte[]> getQemuAudioListener() {
		return this.qemuAudioListener;
	}

	public void setQemuAudioListener(Consumer<byte[]> qemuAudioListener) {
		this.qemuAudioListener = qemuAudioListener;
	}

	public Consumer<Void> getBellListener() {
		return this.bellListener;
	}

	public void setBellListener(Consumer<Void> bellListener) {
		this.bellListener = bellListener;
	}

	public boolean isShared() {
		return this.shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public int getTargetFramesPerSecond() {
		return this.targetFramesPerSecond;
	}

	public void setTargetFramesPerSecond(int targetFramesPerSecond) {
		this.targetFramesPerSecond = targetFramesPerSecond;
	}

	public ColorDepth getColorDepth() {
		return this.colorDepth;
	}

	public void setColorDepth(ColorDepth colorDepth) {
		this.colorDepth = colorDepth;
	}

	public void setUseLocalMousePointer(boolean useLocalMousePointer) {
		this.useLocalMousePointer = useLocalMousePointer;
	}

	public boolean isUseLocalMousePointer() {
		return this.useLocalMousePointer;
	}

	public boolean isEnableCopyrectEncoding() {
		return this.enableCopyrectEncoding;
	}

	public void setEnableCopyrectEncoding(boolean enableCopyrectEncoding) {
		this.enableCopyrectEncoding = enableCopyrectEncoding;
	}

	public boolean isEnableRreEncoding() {
		return this.enableRreEncoding;
	}

	public void setEnableRreEncoding(boolean enableRreEncoding) {
		this.enableRreEncoding = enableRreEncoding;
	}

	public boolean isEnableHextileEncoding() {
		return this.enableHextileEncoding;
	}

	public void setEnableHextileEncoding(boolean enableHextileEncoding) {
		this.enableHextileEncoding = enableHextileEncoding;
	}

	public boolean isEnableZLibEncoding() {
		return this.enableZLibEncoding;
	}

	public void setEnableZLibEncoding(boolean enableZLibEncoding) {
		this.enableZLibEncoding = enableZLibEncoding;
	}

	public boolean isEnableQemuAudioEncoding() {
		return this.enableQemuAudioEncoding;
	}

	public void setEnableQemuAudioEncoding(boolean enableQemuAudioEncoding) {
		this.enableQemuAudioEncoding = enableQemuAudioEncoding;
	}
}
