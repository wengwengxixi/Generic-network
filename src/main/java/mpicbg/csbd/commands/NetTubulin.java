/*-
 * #%L
 * CSBDeep Fiji Plugin: Use deep neural networks for image restoration for fluorescence microscopy.
 * %%
 * Copyright (C) 2017 Deborah Schmidt, Florian Jug, Benjamin Wilhelm
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package mpicbg.csbd.commands;

import java.io.File;
import java.io.IOException;
import java.util.OptionalLong;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imglib2.type.numeric.RealType;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import mpicbg.csbd.tiling.BatchedTiling;
import mpicbg.csbd.tiling.DefaultTiling;

/**
 */
@Plugin( type = Command.class, menuPath = "Plugins>CSBDeep>Deconvolution - Microtubules", headless = true )
public class NetTubulin< T extends RealType< T > > extends CSBDeepCommand< T >
		implements
		Command {

	private static final int BLOCK_MULTIPLE = 4;

	@Parameter( label = "Batch size", min = "1" )
	protected int batchSize = 10;

	@Override
	public void initialize() {

		super.initialize();

		modelFileUrl = "http://csbdeep.bioimagecomputing.com/model-tubulin.zip";
		modelName = "net_tubulin";

	}

	@Override
	protected void initTiling() {
		if ( getInput().numDimensions() == 3 ) {
			final int batchDim = network.getInputNode().getDatasetDimIndexByTFIndex( 0 );
			final int channelDim = network.getInputNode().getDatasetDimensionIndex( Axes.TIME );
			tiling = new BatchedTiling( nTiles, BLOCK_MULTIPLE, overlap, batchSize, batchDim, channelDim );
		} else {
			tiling = new DefaultTiling( nTiles, BLOCK_MULTIPLE, overlap );
		}
	}

	@Override
	public void run() {
		Exception prevException = null;
		try {
			try {
				validateInput(
						getInput(),
						"3D image with dimension order X-Y-T",
						OptionalLong.empty(),
						OptionalLong.empty(),
						OptionalLong.empty() );
			} catch ( final IOException e ) {
				prevException = e;
				validateInput(
						getInput(),
						"2D image with dimension order X-Y",
						OptionalLong.empty(),
						OptionalLong.empty() );
			}
			super.run();
		} catch ( final IOException e ) {
			showError( prevException.getMessage() + "\nOR\n" + e.getMessage() );
		}
	}

	@Override
	protected boolean handleOutOfMemoryError() {
		batchSize /= 2;
		if ( batchSize < 1 ) { return false; }
		return true;
	}

	public static void main( final String... args ) throws Exception {
		// create the ImageJ application context with all available services
		final ImageJ ij = new ImageJ();

		ij.launch( args );

		// ask the user for a file to open
//		final File file = ij.ui().chooseFile( null, "open" );
		final File file =
				new File( "/home/random/Development/imagej/plugins/CSBDeep-data/net_tubulin/input2.tif" );

		if ( file != null && file.exists() ) {
			// load the dataset
			final Dataset dataset = ij.scifio().datasetIO().open( file.getAbsolutePath() );

			// show the image
			ij.ui().show( dataset );

			// invoke the plugin
			ij.command().run( NetTubulin.class, true );
		}

	}
}
