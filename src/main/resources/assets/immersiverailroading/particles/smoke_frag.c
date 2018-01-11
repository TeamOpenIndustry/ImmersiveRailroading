varying float DEPTH;
uniform vec3 DARKEN;
uniform sampler2D tex;

void main() {
	vec3 ct;
	vec4 texel;
	float at,af,ad;

	af = gl_FrontMaterial.diffuse.a;
	
	texel = texture2D(tex,gl_TexCoord[0].st);
	ct = texel.rgb;
	at = texel.a;
	ad = DEPTH;
	at = at * ad;
	gl_FragColor = vec4(ct * DARKEN, at * af);
}