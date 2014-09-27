  Shader "Example/Diffuse Bump" {
    Properties {
      
      _MainTex ("Texture", 2D) = "white" {}
      _BumpMap ("Bumpmap", 2D) = "bump" {}
      _Color ("Main Color", Color) = (1,1,1,1)
    }
    SubShader {
      Tags { "RenderType" = "Opaque" }
      CGPROGRAM
      #pragma surface surf Lambert vertex:vert
      
      
      struct Input {
        float3 vertColor;
        float2 uv_MainTex;
        float2 uv_BumpMap;
        
      };
      
      void vert (inout appdata_full v, out Input o) {
		    UNITY_INITIALIZE_OUTPUT(Input, o);
		    o.vertColor = v.color;
		}
		      
      sampler2D _MainTex;
      sampler2D _BumpMap;
      void surf (Input IN, inout SurfaceOutput o) {
        o.Albedo = tex2D (_MainTex, IN.uv_MainTex).rgb * IN.vertColor;
        o.Normal = UnpackNormal (tex2D (_BumpMap, IN.uv_BumpMap));
      }
      ENDCG
    } 
    Fallback "Diffuse"
  }