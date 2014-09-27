Shader "RSShader" {
    Properties {
        _Color ("Main Color", Color) = (1,1,1,1)
        _SpecColor ("Spec Color", Color) = (1,1,1,1)
        _Emission ("Emmisive Color", Color) = (0,0,0,0)
        _Shininess ("Shininess", Range (0.01, 1)) = 0.7
        _MainTex ("Base (RGB)", 2D) = "white" {}
    }
 
    SubShader {
        Pass {
 			Tags { "LightMode"="ForwardBase"}

            Material {
 
                Emission [_Emission]    
            }
            Cull Off
            ZTest LEqual
            ColorMaterial AmbientAndDiffuse
            Lighting On
            SetTexture [_MainTex] {
                Combine texture * primary, texture * primary
            }
            SetTexture [_MainTex] {
                constantColor [_Color]
                Combine previous * constant DOUBLE, previous * constant
            }
        }
    }
 
    Fallback "VertexLit", 1
}