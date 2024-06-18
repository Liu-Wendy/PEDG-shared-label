#include <iostream>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <fstream>
#include <vector>
#include <cassert>

using namespace std;

ofstream *out;

void print(string str)
{
    (*out)<<str<<endl;
}

int main(int argc, char* argv[])
{
    string s = "10";

    assert(argc == 3);
    int N = atoi(argv[1]);
    assert(N > 1);
    char c = argv[2][0];

    if(c == 'r'){
        s = "10000";
    }
    out=new ofstream("rod_"+to_string(N)+".xml");
    print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sspaceex xmlns=\"http://www-verimag.imag.fr/xml-namespaces/sspaceex\" version=\"0.2\" math=\"SpaceEx\">");
    print("\t<component id=\"rod_template\">\n\t\t<param name=\"x\" type=\"real\" local=\"true\" d1=\"1\" d2=\"1\" dynamics=\"any\" />");
    print("\t\t<param name=\"rod_t0\" type=\"label\" local=\"false\" />");
    print("\t\t<param name=\"add\" type=\"label\" local=\"false\" />");
    print("\t\t<param name=\"remove\" type=\"label\" local=\"false\" />");
    print("\t\t<param name=\"recovery\" type=\"label\" local=\"false\" />");
    print("\t\t<location id=\"1\" name=\"out\">\n\t\t\t<invariant>x&lt;=" + s + "</invariant>\n\t\t\t<flow>x'&lt;=1.1 &amp; x'&gt;=0.9</flow>\n\t\t</location>");
    print("\t\t<location id=\"2\" name=\"in\">\n\t\t\t<invariant>x&lt;=10000</invariant>\n\t\t\t<flow>x'&lt;=1.1 &amp; x'&gt;=0.9</flow>\n\t\t</location>");
    print("\t\t<location id=\"3\" name=\"recover\">\n\t\t\t<invariant>x&lt;=10000</invariant>\n\t\t\t<flow>x'&lt;=1.1 &amp; x'&gt;=0.9</flow>\n\t\t</location>");
    print("\t\t<transition source=\"1\" target=\"2\">\n\t\t\t<label>add</label>\n\t\t\t<guard>x&gt;=1</guard>\n\t\t\t<assignment>x:=0</assignment>\n\t\t</transition>");
    print("\t\t<transition source=\"2\" target=\"3\">\n\t\t\t<label>remove</label>\n\t\t</transition>");
    print("\t\t<transition source=\"3\" target=\"1\">\n\t\t\t<label>recovery</label>\n\t\t</transition>");
    print("\t</component>");
    
    string foot="</sspaceex>";
    print("\t<component id=\"controller_template\">");
    print("\t\t<param name=\"x\" type=\"real\" local=\"true\" d1=\"1\" d2=\"1\" dynamics=\"any\" />");
    print("\t\t<param name=\"t0\" type=\"label\" local=\"false\" />");
    for(unsigned i=0;i<N;i++)
    {
        string name="add_"+to_string(i+1);
        print("\t\t<param name=\""+name+"\" type=\"label\" local=\"false\" />");
        name="remove_"+to_string(i+1);
        print("\t\t<param name=\""+name+"\" type=\"label\" local=\"false\" />");
    }

    //location
    print("\t\t<location id=\"1\" name=\"rod_0\">");
    print("\t\t\t<invariant>x&lt;=16.1</invariant>");
    print("\t\t\t<flow>x'&lt;=1.1 &amp; x'&gt;=0.9</flow>");
    print("\t\t</location>");
    for(unsigned i = 0; i < N; i++){
        print("\t\t<location id=" + to_string(i + 2) + " name=\"rod_" + to_string(i + 1) + "\">");
        print("\t\t\t<invariant>x&lt;=5.9</invariant>");
        print("\t\t\t<flow>x'&lt;=1.1 &amp; x'&gt;=0.9</flow>");
        print("\t\t</location>");
    }

    //transition
    for(unsigned i = 0; i < N; i++){
        print("\t\t<transition source=\"1\" target=\"" + to_string(i + 2) + "\">");
        print("\t\t\t<label>add_" + to_string(i + 1) +"</label>");
        print("\t\t\t<guard>x&gt;=16 &amp; x&lt;=16.1</guard>");
        print("\t\t\t<assignment>x:=0</assignment>");
        print("\t\t</transition>");
    }
    for(unsigned i = 0; i < N; i++){
        print("\t\t<transition source=\"" + to_string(i + 2) + "\" target=\"1\">");
        print("\t\t\t<label>remove_" + to_string(i + 1) +"</label>");
        print("\t\t\t<guard>x&gt;=5 &amp; x&lt;=5.9</guard>");
        print("\t\t\t<assignment>x:=0</assignment>");
        print("\t\t</transition>");
    }
    print("\t</component>");

    print("\t<component id=\"system\">");
    print("\t\t<param name=\"t0\" type=\"label\" local=\"false\" />");
    for(unsigned i=0;i<N;i++)
    {
        string name= to_string(i+1);
        print("\t\t<param name=\"rod_"+name+"_t0\" type=\"label\" local=\"false\" />");
        print("\t\t<param name=\"add_"+name+"\" type=\"label\" local=\"false\" />");
        print("\t\t<param name=\"remove_"+name+"\" type=\"label\" local=\"false\" />");
        print("\t\t<param name=\"recovery_"+name+"\" type=\"label\" local=\"false\" />");
    }

    for(unsigned i = 0; i < N; i++){
        string id = to_string(i + 1);
        print("\t\t<bind component=\"rod_template\" as=\"rod_"+id+"\">");
        print("\t\t\t<map key=\"rod_t0\">rod_" + id + "_t0</map>");
        print("\t\t\t<map key=\"add\">add_" + id + "</map>");
        print("\t\t\t<map key=\"remove\">remove_" + id + "</map>");
        print("\t\t\t<map key=\"recovery\">recovery_" + id + "</map>");
        print("\t\t</bind>");
    }

    print("\t\t<bind component=\"controller_template\" as=\"controller\">");
    print("\t\t\t<map key=\"t0\">t0</map>");
    for(unsigned i = 0; i < N; i++){
        string id = to_string(i + 1);
        print("\t\t\t<map key=\"add_" + id + "\">add_" + id + "</map>");
        print("\t\t\t<map key=\"remove_" + id + "\">remove_" + id + "</map>");
    }
    
    
    print("\t\t</bind>");
    print("\t</component>");
    print(foot);
    out->close();
    out=new ofstream("rod_"+to_string(N)+".cfg");

    print("# analysis options");
    print("system = \"system\"");
    string init="controller.x==0&loc(controller)==rod_0";
    string forbid = "loc(controller)==rod_0";
    for(unsigned i = 0; i < N; i++){
        string id = to_string(i + 1);
        init += "&rod_" + id + ".x==0";
        init += "&loc(rod_" + id + ")==out";
        forbid += "&loc(rod_" + id + ")==recover";
    }
    
    print("initially = \""+init+"\"");
    print("forbidden=\""+forbid+"\"");

    print("iter-max = " + to_string(2 * N + 1));
    
    print("rel-err = 1.0e-12");
    print("abs-err = 1.0e-13");

    out->close();
    return 0;
}
