uniform float ALPHA;
uniform vec3 DARKEN;
uniform sampler2D tex;

varying vec4 v_texCoord;

void main() {
	vec3 ct,tmp;
	vec4 texel;
	float at,af,ad;
	af = sin(v_texCoord.x * 3.1415);
	ad = sin(v_texCoord.y * 3.1415);
	at = (af * ad + ( sin(v_texCoord.x * 20.0) * sin(v_texCoord.y * 20.0) / 20.0 )) * ALPHA;
	tmp = DARKEN * ( (v_texCoord.x + 1.0) / 2.0 );
	gl_FragColor = vec4(tmp, at);
}