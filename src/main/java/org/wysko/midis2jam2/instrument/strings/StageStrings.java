package org.wysko.midis2jam2.instrument.strings;

import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class StageStrings extends Instrument {
	
	// Strings are 9 / 12 degrees apart
	// Left one is 22 up
	
	private final List<NotePeriod> notePeriods;
	private final List<MidiChannelSpecificEvent> events;
	OneStageString[] strings = new OneStageString[12];
	Node[] stringNodes = new Node[12];
	Node highestLevel = new Node();
	
	public StageStrings(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context);
		this.events = eventList;
		List<MidiNoteEvent> midiNoteEvents = scrapeMidiNoteEvents(eventList);
		notePeriods = calculateNotePeriods(midiNoteEvents);
		
		for (int i = 0; i < 12; i++) {
			stringNodes[i] = new Node();
			strings[i] = new OneStageString();
			stringNodes[i].attachChild(strings[i].highestLevel);
			strings[i].highestLevel.setLocalTranslation(0, 2*i, -151.76f);
			stringNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad((9/10f) * i), 0));
			highestLevel.attachChild(stringNodes[i]);
		}
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		final int i1 =
				context.instruments.stream().filter(e -> e instanceof StageStrings).collect(Collectors.toList()).indexOf(this);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(0,rad(35.6 + (10.6 * i1)),0));
		
		/* Collect note periods to execute */
		List<NotePeriod> currentNotePeriods = new ArrayList<>();
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
		}
		
		if (!currentNotePeriods.isEmpty()) {
			for (NotePeriod currentNotePeriod : currentNotePeriods) {
				int midiNote = currentNotePeriod.midiNote;
				int stringIndex = 11 - ((midiNote + 3) % 12);
				strings[stringIndex].play(currentNotePeriod.duration());
			}
		}
		
		// Tick each string
		Arrays.stream(strings).forEach(string -> string.tick(time, delta));
	}
	
	public class OneStageString {
		final Node highestLevel = new Node();
		final Node movementNode = new Node();
		final Node bowNode = new Node();
		final Spatial[] animStrings = new Spatial[5];
		Node animStringNode = new Node();
		final Spatial restingString;
		final Spatial bow;
		double progress = 0;
		boolean playing = false;
		double duration = 0;
		
		double frame = 0;
		
		public void play(double duration) {
			playing = true;
			progress = 0;
			this.duration = duration;
		}
		
		public void tick(double time, float delta) {
			if (progress >= 1) {
				playing = false;
				progress = 0;
			}
			if (playing) {
				progress += delta / duration;
				bowNode.setCullHint(Spatial.CullHint.Dynamic);
				bow.setLocalTranslation(0,(float) (8 * (progress - 0.5)),0);
				movementNode.setLocalTranslation(0,0,2);
				
				restingString.setCullHint(Spatial.CullHint.Always);
				animStringNode.setCullHint(Spatial.CullHint.Dynamic);
			} else {
				bowNode.setCullHint(Spatial.CullHint.Always);
				movementNode.setLocalTranslation(0,0,0);
				
				restingString.setCullHint(Spatial.CullHint.Dynamic);
				animStringNode.setCullHint(Spatial.CullHint.Always);
			}
			calculateFrameChanges(delta);
			animateStrings();
		}
		
		private void animateStrings() {
			for (int i = 0; i < 5; i++) {
				frame = frame % 5;
				if (i == Math.floor(frame)) {
					animStrings[i].setCullHint(Spatial.CullHint.Dynamic);
				} else {
					animStrings[i].setCullHint(Spatial.CullHint.Always);
				}
			}
		}
		
		protected void calculateFrameChanges(float delta) {
			final double inc = delta / 0.016666668f;
			this.frame += inc;
		}
		
		public OneStageString() {
			// Load holder
			movementNode.attachChild(context.loadModel("StageStringHolder.obj", "FakeWood.bmp",
					Midis2jam2.MatType.UNSHADED, 0));
			
			// Load anim strings
			for (int i = 0; i < 5; i++) {
				animStrings[i] = context.loadModel("StageStringBottom" + i + ".obj", "StageStringPlaying.bmp",
						Midis2jam2.MatType.UNSHADED, 0);
				animStrings[i].setCullHint(Spatial.CullHint.Always);
				animStringNode.attachChild(animStrings[i]);
			}
			movementNode.attachChild(animStringNode);
			
			// Load resting string
			restingString = context.loadModel("StageString.obj", "StageString.bmp", Midis2jam2.MatType.UNSHADED, 0);
			movementNode.attachChild(restingString);
			
			// Load bow
			bow = context.loadModel("StageStringBow.fbx", "FakeWood.bmp", Midis2jam2.MatType.UNSHADED, 0);
			Node bowAsNode = (Node) this.bow;
			bowAsNode.getChild(1).setMaterial(((Geometry) restingString).getMaterial());
			bowNode.attachChild(this.bow);
			bowNode.setLocalTranslation(0, 48, 0);
			bowNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-60)));
			bowNode.setCullHint(Spatial.CullHint.Always);
			movementNode.attachChild(bowNode);
			
			highestLevel.attachChild(movementNode);
		}
	}
}
