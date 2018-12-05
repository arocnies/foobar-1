package foobar

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

import javax.inject.Inject

@Controller( "/oms" )
class OmsController
{
	@Inject
	OmsService omsService
	
	@Get( "/getOrtho" )
	HttpResponse<byte[]> index()
	{
		byte[] bytes = omsService.getOrtho()
		
		return HttpResponse.ok( bytes ).contentLength( bytes.length ).contentType( 'image/jpeg' )
	}
}