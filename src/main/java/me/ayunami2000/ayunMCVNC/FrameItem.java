package me.ayunami2000.ayunMCVNC;

public class FrameItem {
	public DisplayInfo display;
	public byte[][] frameBuffer;

	public FrameItem(DisplayInfo display, byte[][] frameBuffer) {
		this.display = display;
		this.frameBuffer = frameBuffer;
	}
}
