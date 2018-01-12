
varying vec4 v_texCoord;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;
    v_texCoord = gl_MultiTexCoord0;
}