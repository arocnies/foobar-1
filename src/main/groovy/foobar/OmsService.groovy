package foobar

import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import joms.oms.Chipper
import joms.oms.Init
import joms.oms.ossimInterleaveType
import org.apache.commons.io.output.ByteArrayOutputStream as FastByteArrayOutputStream
import org.ossim.oms.image.ImageDataBuffer

import javax.imageio.ImageIO
import javax.inject.Singleton
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.PixelInterleavedSampleModel
import java.awt.image.Raster

@Singleton
class OmsService
{
	byte[] getOrtho()
	{
		BufferedImage image = ortho()
		FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream( image.width * image.height * image.sampleModel.numBands )
		
		ImageIO.write( image, 'jpeg', outputStream )
		
		return outputStream.toByteArray()
	}
	
	private BufferedImage blank()
	{
		BufferedImage image = new BufferedImage( 256, 256, BufferedImage.TYPE_INT_RGB )
		image
	}
	
	private BufferedImage ortho()
	{
		BufferedImage image
		
		Map<String, String> opts = [
			operation: 'ortho',
			cut_width: '1024',
			cut_height: '512',
			srs: 'epsg:4326',
			cut_wms_bbox: '-180,-90,180,90'
		]
		
		new File( '/data/bmng' ).listFiles( { f -> f.name ==~ /.*tif/ } as FileFilter ).eachWithIndex { f, i ->
			opts["image${ i }.file"] = f.absolutePath
		}
		
		Chipper chipper = new Chipper()
		
		if ( chipper.initialize( opts ) )
		{
			println 'initialized'
			
			def chip = chipper.getChip( opts )
			
			if ( chip && chip.valid() )
			{
				println 'got chip'
				
				def dataBuffer = new ImageDataBuffer( chip, ossimInterleaveType.OSSIM_BIP )?.dataBuffer
				def width = chip.width.toInteger()
				def height = chip.height.toInteger()
				def pixelStride = chip.numberOfBands.toInteger()
				def scanlineStride = pixelStride * width
				
				def sampleModel = new PixelInterleavedSampleModel( dataBuffer.dataType,
					width, height, pixelStride, scanlineStride,
					( 0..<chip.numberOfBands ) as int[] )
				
				def raster = Raster.createWritableRaster( sampleModel, dataBuffer, null )
				def colorSpace = ColorSpace.getInstance( ( chip.numberOfBands == 1 ) ? ColorSpace.CS_GRAY : ColorSpace.CS_sRGB )
				def colorModel = new ComponentColorModel( colorSpace, false, false, ComponentColorModel.OPAQUE, dataBuffer.dataType )
				
				image = new BufferedImage( colorModel, raster, colorModel.isAlphaPremultiplied(), null )
			}
			else
			{
				System.err.print "Chipper can't get chip"
			}
		}
		else
		{
			System.err.print "Chipper didn't initialize"
		}
		
		chipper.delete()
		
		return image
	}
	
	@EventListener
	void onStartup( StartupEvent event )
	{
		println 'initializing'
		Init.instance().initialize()
		
		ImageIO.useCache = false
	}
}