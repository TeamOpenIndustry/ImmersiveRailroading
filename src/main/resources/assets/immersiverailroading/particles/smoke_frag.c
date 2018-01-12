uniform float ALPHA;
uniform vec3 DARKEN;
uniform sampler2D tex;

varying vec4 v_texCoord;

void main() {
	vec3 ct,tmp;
	vec4 texel;
	float at,af,ad;
	af = (-pow(((v_texCoord.x*2.0)-1.0), 4.0)+1.0);
	ad = (-pow(((v_texCoord.y*2.0)-1.0), 4.0)+1.0);
	at = (af * ad + ( sin(v_texCoord.x * 20.0) * sin(v_texCoord.y * 20.0) / 20.0 )) * ALPHA;
	tmp = DARKEN * ( (v_texCoord.x + 19.0) / 20.0 );
	gl_FragColor = vec4(tmp, at);
}