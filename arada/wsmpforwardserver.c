/*WSMPFORWARDSERVER - A sample application which can run on remote host to decode
 * UDP packets send by wsmpforward application
 * arguement : port number to listen on
 */

#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <signal.h>

typedef unsigned long int uint32;
typedef unsigned short int uint16;
typedef unsigned char uint8;
typedef signed char int8;

#define SECURITY 1	//193
#define DIGEST 	2 	//194
#define MSG_CNT_DIGEST 92	//213
#define MSG_CNT_CERT 21		//284
#define MSG_CNT_PLAIN 9		//201
#define DIGEST_DATA_OFFSET 84
#define CERT_DATA_OFFSET 13

#ifdef WIN32
#include <winsock2.h>
#include <mswsock.h>
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include "os.h"

/*Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib*/
#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")

#define	WIN_SOCK_DLL_INVOKE	{WSADATA wsaData;\
		int iResult;\
		iResult=WSAStartup(MAKEWORD(2,2),&wsaData);\
		if(iResult!=0)\
			printf("WSASTARTUP Failed: %d\n",iResult);\
	}

//#pragma pack(1)
struct GenericDataLong {
    u_int16_t length;
    char contents[1300];
};

typedef struct GenericDataLong WSMData;

struct edcaparam {
    u_int8_t aifsn;
    u_int8_t logcwmin;
    u_int8_t logcwmax;
    u_int16_t txopLimit;
    u_int8_t acm;
};

#pragma pack(1)
struct channelinfo {
    u_int8_t reg_class;
    u_int8_t channel;
    u_int8_t adaptable;
    u_int8_t rate;
    int8_t txpower;
    struct edcaparam edca[4];
    u_int8_t channelaccess;
} ;

#pragma pack(1)
struct wsm_indication {
    struct channelinfo chaninfo;
    u_int32_t psid;
    u_int8_t version;
    u_int8_t txpriority;
    u_int8_t wsmps;
    u_int8_t security;
    WSMData data;
    u_int8_t macaddr[6];
    u_int8_t rssi;
};
#else
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <endian.h>

struct GenericDataLong {
    u_int16_t length;
    char contents[1300];
} __attribute__((__packed__));

typedef struct GenericDataLong WSMData;

struct edcaparam {
    u_int8_t aifsn;
    u_int8_t logcwmin;
    u_int8_t logcwmax;
    u_int16_t txopLimit;
    u_int8_t acm;
};

struct channelinfo {
    u_int8_t reg_class;
    u_int8_t channel;
    u_int8_t adaptable;
    u_int8_t rate;
    int8_t txpower;
    struct edcaparam edca[4];
    u_int8_t channelaccess;
}  __attribute__((__packed__));

struct wsm_indication {
    struct channelinfo chaninfo;
    u_int32_t psid;
    u_int8_t version;
    u_int8_t txpriority;
    u_int8_t wsmps;
    u_int8_t security;
    WSMData data;
    u_int8_t macaddr[6];
    u_int8_t rssi;
} __attribute__((__packed__));

#endif

typedef struct wsm_indication WSMIndication;

WSMIndication Ind;

FILE *fd_kml;

extern const struct in6_addr in6addr_any;

void sig_handler(int signo)
{
	if(signo == SIGINT){
	printf("\nSIGINT\n");
	fclose(fd_kml);
	exit(0);
	}
	else{
	printf("\nUnknown interrupt: %d\n",signo);
	exit(0);
	}
}

void fill_buffer(char *buffer,int sig,int tmpid)
{
	char string1[] = "<?xml version=\"1.0\" standalone=\"yes\"?>";
	char string2[] = "<kml xmlns=\"http://earth.google.com/kml/2.2\">";
	char string3[] = "<Document>";
	char string4[] = "<name><![CDATA[wsmpforwardserver]]></name>";
	char string5[] = "<visibility>1</visibility>";
	char string6[] = "<open>1</open>";
	char string7[] = "<Snippet></Snippet>";
	char string8[] = "<Folder id=\"Tracks\">";
	char string9[] = "<name>Tracks</name>";
	char string10[] = "<visibility>1</visibility>";
	char string11[] = "<open>0</open>";
	char string12[] = "<Placemark>";
	char string13[60] ;//= "<name><![CDATA[wsmpforwardserver]]></name>";
	char string14[] = "<Snippet></Snippet>";
	char string15[] = "<description><![CDATA[&nbsp;]]></description>";
	char string16[] = "<Style>";
	char string17[] = "<LineStyle>";
	char string18[] = "<color>FF0000E6</color>";
	char string19[] = "<width>2</width>";
	char string20[] = "</LineStyle>";
	char string21[] = "</Style>";
	char string22[] = "<MultiGeometry>";
	char string23[] = "<LineString>";
	char string24[] = "<tessellate>1</tessellate>";
	char string25[] = "<altitudeMode>clampToGround</altitudeMode>";
	char string26[] = "<coordinates>";
	char string27[] = "</coordinates>";
	char string28[] = "</LineString>";
	char string29[] = "</MultiGeometry>";
	char string30[] = "</Placemark>";
	char string31[] = "</Folder>";
	char string32[] = "</Document>";
	char string33[] = "</kml>";
	
	//sig = 0 start, sig = 1 end
	if(sig == 0){
	sprintf(string13,"<name><![CDATA[devid : %x]]></name>",tmpid);
	//create start buffer
	sprintf(buffer,"%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",string1,string2,string3,string4,string5,string6,string7,string8,string9,string10,string11,string12,string13,string14,string15,string16,string17,string18,string19,string20,string21,string22,string23,string24,string25,string26);
	}
	else if(sig == 1){
	//create end buffer
	sprintf(buffer,"%s\n%s\n%s\n%s\n%s\n%s\n%s",string27,string28,string29,string30,string31,string32,string33);
	}
	else{
	printf("\nInvalid token\n");
	}
}

int extract_data(WSMData *data,char *kml_lonlat)
{
	int msg_cnt = 0;
	int i;
	unsigned char * buffer;
	int lat ,lon;
	int lat1 ,lon1;
	int tmpid=0,tmpid1=0;
	double latrx,lonrx;

/*	for(i=0;i<3;i++)
	{
	printf("\ndata[%d] : %x %x %x %x %x %x %x %x \n",i*8,*(unsigned char *)&(data->contents[i*8]),*(unsigned char *)&(data->contents[ i*8 + 1]),*(unsigned char *)&(data->contents[i*8 + 2]),*(unsigned char *)&(data->contents[i*8 + 3]),*(unsigned char *)&(data->contents[i*8 + 4]),*(unsigned char *)&(data->contents[i*8 + 5]),*(unsigned char *)&(data->contents[i*8 + 6]),*(unsigned char *)&(data->contents[i*8 + 7]));
	}
*/
	if(*(unsigned char *)&data->contents[SECURITY] == 0x00)
	{
	msg_cnt = MSG_CNT_PLAIN;
	if(*(unsigned char *)&data->contents[SECURITY + 2] > 0x80)
        msg_cnt = msg_cnt+1;        
	}
	else if (*(unsigned char *)&data->contents[SECURITY] != 0x00)
	{
		if(*(unsigned char *)&data->contents[DIGEST] == 0x03)
		{
		msg_cnt = MSG_CNT_DIGEST;
		if(*(unsigned char *)&data->contents[84] > 0x7f)
	        msg_cnt = msg_cnt+2;
		}
		else if (*(unsigned char *)&data->contents[DIGEST] == 0x02)
		{
		msg_cnt = MSG_CNT_CERT;
		if(*(unsigned char *)&data->contents[13] > 0x7f)
	        msg_cnt = msg_cnt+2;        
		}
	}
	
	buffer = (unsigned char *)(data->contents + msg_cnt) ;

	//copy lat lon from buffer: msg_cnt+7 is lat, msg_cnt+11 is lon
	memcpy(&lat,buffer+7,4);
   	memcpy(&lon,buffer+11,4);

	//convert big endian to host
	lat1 = ntohl(lat);
	lon1 = ntohl(lon);

	latrx = (double)lat1/10000000;
	lonrx = (double)lon1/10000000;	

	//printf("\nRecieved lat , lon  = %lf , %lf\n", latrx,lonrx);

	//get tempid
	//memcpy(&tmpid,buffer+1,4);
	//printf("\ntmpid: %x %x %x %x\n",*(unsigned char *)(buffer+1),*(unsigned char *)(buffer+1+1),*(unsigned char *)(buffer+1+2),*(unsigned char *)(buffer+1+3));
	memcpy(&tmpid,buffer+1,2);//taken first two bytes for dev_id else tempid is 4 bytes
	tmpid1 = ntohl(tmpid);
	tmpid1 =((0x0000FFFF)&(tmpid1 >> 16)); //first 2 bytes are needed

	//data for kml file: bufferlonlat
	sprintf(kml_lonlat,"%lf,%lf,0 ",lonrx,latrx);
	return tmpid1;
}


int main(int argc, char *argv[] )
{
    int sock,len,i,tempid,first_data=0;
    int addr_len, bytes_read=0;
    uint16_t listen_port;
    unsigned int count=0;
    struct sockaddr_in6 server_addr, client_addr;

    int kml_check = 0;
    char bufferstart[1000];
    char bufferlonlat[50];
    char bufferend[1000];
    long position;

    if(argc < 2) {
        printf("\nUsage :: executable  port_number[mandatory] KML_Filename[optional]\n");
	printf("\nPort_no : to listen client\nKML_Filename :: In (.kml format), to generate kml file for google earth\n");
        exit(0);
    }
    else{
	if(argc >= 3){
	    kml_check = 1;
            if(signal(SIGINT,sig_handler) == SIG_ERR){
	        printf("\nCAnt catch SIGINT\n");
	    }
	}
    }
#ifdef WIN32
	WIN_SOCK_DLL_INVOKE;
#endif

    listen_port = atoi(argv[1]);
    if ((sock = socket(AF_INET6, SOCK_DGRAM, 0)) == -1) {
        perror("Socket");
        exit(1);
    }

    server_addr.sin6_family = AF_INET6;
    server_addr.sin6_addr= in6addr_any;
    server_addr.sin6_port = htons(listen_port);
    server_addr.sin6_scope_id = 0;

    if (bind(sock,(struct sockaddr *)&server_addr,
            sizeof(struct sockaddr_in6)) == -1)
    {
        perror("Bind");
        exit(1);
    }

    addr_len = sizeof(struct sockaddr_in6);

    printf("\nWSMPForward-Server Waiting for client on port %d\n",listen_port);
    fflush(stdout);

    if(kml_check == 1){
    //open kml file with fd_kml
    fd_kml = fopen(argv[2],"w+");
    if(fd_kml == NULL){
	printf("\ncould not open kml write file\n");
	exit(0);
    }

    //create start buffer for kml
    //fill_buffer(bufferstart,0);	
    //fputs(bufferstart,fd_kml);
    //position = ftell(fd_kml); //retain fd_kml position to write data where bufferstart ends

    //create end buffer for kml
    fill_buffer(bufferend,1,0);
    //fputs(bufferend,fd_kml);

    fflush(fd_kml);
    }
	
    while (1)
    {
        len=sizeof(Ind);
        bytes_read = recvfrom(sock,&Ind,len,0,
            (struct sockaddr *)&client_addr, &addr_len);
		
		if(kml_check == 1){
		//extract lat-lon from recieved data
		tempid = extract_data(&Ind.data,bufferlonlat);
		
		if(first_data == 0){
		    //create start buffer for kml
		    fill_buffer(bufferstart,0,tempid);
		    fputs(bufferstart,fd_kml);
		    position = ftell(fd_kml); //retain fd_kml position to write data where bufferstart ends
		    first_data++;
		}	

		//get fd_kml to position where next coordinates is to be added
		fseek(fd_kml,position,SEEK_SET);
		fputs(bufferlonlat,fd_kml);
		
		position = ftell(fd_kml); //retain fd_kml position to write next coordinates
		fputs(bufferend,fd_kml); // put bufferend data to complete kml tags.
        
		fflush(fd_kml);
		fclose(fd_kml);
		fd_kml = fopen(argv[2],"r+");
		if(fd_kml == NULL){
        	    printf("\ncould not open kml write file\n");
        	    exit(0);
    		}
	}

        printf("contents -- ");
	for(i=0; i<8; i++) //printing first 8 bytes of actual data
        {
            printf("%02x ",(Ind.data.contents[i] & 0xff));
        }
        printf(" ; data-len %u ; pktcount %u\n",Ind.data.length,++count);
        fflush(stdout);

    }
    fclose(fd_kml);
    return 0;
}
