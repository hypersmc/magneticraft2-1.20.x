#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out float projectedY;
out vec3 viewNormal;
out vec3 viewDirection;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    vertexDistance = length(viewPos.xyz);
    vertexColor = Color;
    texCoord0 = UV0;

    // Incoming Position is already transformed by the block-entity pose
    // stack on the CPU, so its Y is continuous over the whole scene.
    projectedY = Position.y;
    viewNormal = normalize(mat3(ModelViewMat) * Normal);
    viewDirection = normalize(-viewPos.xyz);
}
