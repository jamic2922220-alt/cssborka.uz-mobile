/* X CLIENT (iPhone): birinchi ochilishda o'yin fayllari (cs16_data.zip, Android bilan bir xil) yuklanadi va
 * Documents ga ochiladi, keyin Counter-Strike o'zi boshlanadi. Bu fayl CI da FWGS ning ios/Launcher/AppDelegate.m
 * ichiga #include qilinadi (Xcode loyihasini o'zgartirmaslik uchun). zlib to'g'ridan-to'g'ri bog'lanmaydi - dlsym. */
#include <zlib.h>
#include <sys/stat.h>
#include <errno.h>

#define XC_DATA_URL  @"https://mmb.boost-top.com/mobile/cs16_data.zip"
#define XC_DATA_VER  @"1"      /* arxiv yangilansa oshiriladi - qayta yuklanadi */

typedef int (*xc_inflateInit2_t)(z_streamp, int, const char *, int);
typedef int (*xc_inflate_t)(z_streamp, int);
typedef int (*xc_inflateEnd_t)(z_streamp);

static uint16_t xc_rd16(const unsigned char *p) { return (uint16_t)(p[0] | (p[1] << 8)); }
static uint32_t xc_rd32(const unsigned char *p) { return (uint32_t)p[0] | ((uint32_t)p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24); }

static void xc_mkdirs(char *path)   /* path ichidagi hamma papkalar (oxirgi "/" gacha) */
{
	for (char *s = path + 1; *s; s++)
		if (*s == '/') { *s = 0; mkdir(path, 0755); *s = '/'; }
}

/* 0 - muvaffaqiyat; prog: 0..1 */
static int xc_unzip(const char *zip, const char *dst, volatile float *prog)
{
	void *z = dlsym(RTLD_DEFAULT, "inflateInit2_");
	void *lib = NULL;
	if (!z) { lib = dlopen("/usr/lib/libz.1.dylib", RTLD_NOW); if (!lib) return -10; }
	xc_inflateInit2_t pInit = (xc_inflateInit2_t)dlsym(lib ? lib : RTLD_DEFAULT, "inflateInit2_");
	xc_inflate_t pInf = (xc_inflate_t)dlsym(lib ? lib : RTLD_DEFAULT, "inflate");
	xc_inflateEnd_t pEnd = (xc_inflateEnd_t)dlsym(lib ? lib : RTLD_DEFAULT, "inflateEnd");
	if (!pInit || !pInf || !pEnd) return -11;

	FILE *f = fopen(zip, "rb");
	if (!f) return -1;
	fseeko(f, 0, SEEK_END);
	off_t sz = ftello(f);
	off_t scan = sz < 65557 ? sz : 65557;
	unsigned char *tail = malloc((size_t)scan);
	fseeko(f, sz - scan, SEEK_SET);
	if (fread(tail, 1, (size_t)scan, f) != (size_t)scan) { free(tail); fclose(f); return -2; }
	long eocd = -1;
	for (long i = (long)scan - 22; i >= 0; i--)
		if (tail[i] == 'P' && tail[i + 1] == 'K' && tail[i + 2] == 5 && tail[i + 3] == 6) { eocd = i; break; }
	if (eocd < 0) { free(tail); fclose(f); return -3; }
	uint32_t n = xc_rd16(tail + eocd + 10), cdsz = xc_rd32(tail + eocd + 12), cdoff = xc_rd32(tail + eocd + 16);
	free(tail);
	unsigned char *cd = malloc(cdsz);
	fseeko(f, cdoff, SEEK_SET);
	if (fread(cd, 1, cdsz, f) != cdsz) { free(cd); fclose(f); return -4; }

	const size_t CH = 256 * 1024;
	unsigned char *ib = malloc(CH), *ob = malloc(CH);
	char path[2048];
	int rc = 0;
	unsigned char *p = cd;
	for (uint32_t k = 0; k < n && !rc; k++)
	{
		if (p + 46 > cd + cdsz || xc_rd32(p) != 0x02014b50) { rc = -5; break; }
		uint16_t meth = xc_rd16(p + 10), nl = xc_rd16(p + 28), el = xc_rd16(p + 30), cl = xc_rd16(p + 32);
		uint32_t csz = xc_rd32(p + 20), lho = xc_rd32(p + 42);
		char name[1024];
		size_t L = nl < sizeof(name) - 1 ? nl : sizeof(name) - 1;
		memcpy(name, p + 46, L); name[L] = 0;
		p += 46 + nl + el + cl;
		for (char *c = name; *c; c++) if (*c == '\\') *c = '/';
		if (!name[0] || name[0] == '/' || strstr(name, "..")) continue;   /* xavfli yo'l */
		snprintf(path, sizeof(path), "%s/%s", dst, name);
		if (name[strlen(name) - 1] == '/') { xc_mkdirs(path); continue; }
		xc_mkdirs(path);

		unsigned char lh[30];
		fseeko(f, lho, SEEK_SET);
		if (fread(lh, 1, 30, f) != 30 || xc_rd32(lh) != 0x04034b50) { rc = -6; break; }
		fseeko(f, (off_t)lho + 30 + xc_rd16(lh + 26) + xc_rd16(lh + 28), SEEK_SET);
		FILE *o = fopen(path, "wb");
		if (!o) { rc = -7; break; }
		uint32_t left = csz;
		if (meth == 0)
		{
			while (left && !rc) { size_t r = fread(ib, 1, left < CH ? left : CH, f); if (!r) rc = -8; else { fwrite(ib, 1, r, o); left -= (uint32_t)r; } }
		}
		else if (meth == 8)
		{
			z_stream zs; memset(&zs, 0, sizeof(zs));
			if (pInit(&zs, -MAX_WBITS, ZLIB_VERSION, (int)sizeof(z_stream)) != Z_OK) rc = -9;
			int zr = Z_OK;
			while (!rc && zr != Z_STREAM_END)
			{
				if (!zs.avail_in && left) { size_t r = fread(ib, 1, left < CH ? left : CH, f); if (!r) { rc = -8; break; } left -= (uint32_t)r; zs.next_in = ib; zs.avail_in = (uInt)r; }
				zs.next_out = ob; zs.avail_out = (uInt)CH;
				zr = pInf(&zs, Z_NO_FLUSH);
				if (zr != Z_OK && zr != Z_STREAM_END) { rc = -12; break; }
				fwrite(ob, 1, CH - zs.avail_out, o);
				if (zr == Z_OK && !zs.avail_in && !left && zs.avail_out) { rc = -13; break; }   /* arxiv kesilgan */
			}
			pEnd(&zs);
		}
		fclose(o);
		if (prog) *prog = (float)(k + 1) / (float)n;
	}
	free(ib); free(ob); free(cd); fclose(f);
	return rc;
}

@interface XCSetup : NSObject <NSURLSessionDownloadDelegate>
@property (nonatomic) UIAlertController *alert;
@property (nonatomic) NSURLSession *session;
@property (nonatomic, copy) void (^onReady)(void);
@end

@implementation XCSetup
+ (NSString *)doc { return documentsDirctory.path; }
+ (BOOL)ready
{
	NSString *d = [self doc];
	NSString *v = [NSString stringWithContentsOfFile:[d stringByAppendingPathComponent:@".xc_data_ver"] encoding:NSUTF8StringEncoding error:nil];
	return [v isEqualToString:XC_DATA_VER]
		&& [NSFileManager.defaultManager fileExistsAtPath:[d stringByAppendingPathComponent:@"valve/liblist.gam"]]
		&& [NSFileManager.defaultManager fileExistsAtPath:[d stringByAppendingPathComponent:@"cstrike/liblist.gam"]];
}
- (UIViewController *)top
{
	UIViewController *vc = UIApplication.sharedApplication.delegate.window.rootViewController;
	while (vc.presentedViewController) vc = vc.presentedViewController;
	return vc;
}
- (void)say:(NSString *)msg
{
	dispatch_async(dispatch_get_main_queue(), ^{
		if (self.alert) { self.alert.message = msg; return; }
		self.alert = [UIAlertController alertControllerWithTitle:@"X CLIENT" message:msg preferredStyle:UIAlertControllerStyleAlert];
		[[self top] presentViewController:self.alert animated:YES completion:nil];
	});
}
- (void)fail:(NSString *)msg
{
	dispatch_async(dispatch_get_main_queue(), ^{
		void (^show)(void) = ^{
			UIAlertController *a = [UIAlertController alertControllerWithTitle:@"X CLIENT" message:msg preferredStyle:UIAlertControllerStyleAlert];
			[a addAction:[UIAlertAction actionWithTitle:@"Qayta urinish" style:UIAlertActionStyleDefault handler:^(UIAlertAction *x) { [self start]; }]];
			[[self top] presentViewController:a animated:YES completion:nil];
		};
		if (self.alert) { UIAlertController *old = self.alert; self.alert = nil; [old dismissViewControllerAnimated:NO completion:show]; }
		else show();
	});
}
- (void)start
{
	[self say:@"O'yin fayllari yuklanmoqda... (~280 MB, Wi-Fi tavsiya etiladi)"];
	NSURLSessionConfiguration *cfg = NSURLSessionConfiguration.defaultSessionConfiguration;
	cfg.timeoutIntervalForResource = 60 * 60;
	self.session = [NSURLSession sessionWithConfiguration:cfg delegate:self delegateQueue:nil];
	[[self.session downloadTaskWithURL:[NSURL URLWithString:XC_DATA_URL]] resume];
}
- (void)URLSession:(NSURLSession *)s downloadTask:(NSURLSessionDownloadTask *)t didWriteData:(int64_t)w totalBytesWritten:(int64_t)tw totalBytesExpectedToWrite:(int64_t)te
{
	static int last = -1;
	int pct = te > 0 ? (int)(tw * 100 / te) : 0;
	if (pct == last) return;
	last = pct;
	[self say:[NSString stringWithFormat:@"O'yin fayllari yuklanmoqda: %d%%\n%lld / %lld MB", pct, tw >> 20, te >> 20]];
}
- (void)URLSession:(NSURLSession *)s downloadTask:(NSURLSessionDownloadTask *)t didFinishDownloadingToURL:(NSURL *)loc
{
	NSHTTPURLResponse *r = (NSHTTPURLResponse *)t.response;
	if (r.statusCode != 200) { [self fail:[NSString stringWithFormat:@"Yuklab bo'lmadi (xato %ld). Internetni tekshiring.", (long)r.statusCode]]; return; }
	NSString *zip = [[XCSetup doc] stringByAppendingPathComponent:@".xc_data.zip"];
	[NSFileManager.defaultManager removeItemAtPath:zip error:nil];
	NSError *e = nil;
	if (![NSFileManager.defaultManager moveItemAtURL:loc toURL:[NSURL fileURLWithPath:zip] error:&e]) { [self fail:@"Telefonda joy yetmadi (kamida 1 GB bo'sh joy kerak)."]; return; }
	[self say:@"Fayllar ochilmoqda..."];
	__block volatile float prog = 0;
	__block BOOL done = NO;
	dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
		int rc = xc_unzip(zip.fileSystemRepresentation, [XCSetup doc].fileSystemRepresentation, &prog);
		done = YES;
		[NSFileManager.defaultManager removeItemAtPath:zip error:nil];
		if (rc) { [self fail:[NSString stringWithFormat:@"Fayllarni ochib bo'lmadi (%d). Joy yetarlimi? Qayta urinib ko'ring.", rc]]; return; }
		[XC_DATA_VER writeToFile:[[XCSetup doc] stringByAppendingPathComponent:@".xc_data_ver"] atomically:YES encoding:NSUTF8StringEncoding error:nil];
		dispatch_async(dispatch_get_main_queue(), ^{
			UIAlertController *a = self.alert; self.alert = nil;
			[a dismissViewControllerAnimated:YES completion:^{ if (self.onReady) self.onReady(); }];
		});
	});
	dispatch_async(dispatch_get_global_queue(QOS_CLASS_UTILITY, 0), ^{
		while (!done) { [self say:[NSString stringWithFormat:@"Fayllar ochilmoqda: %d%%", (int)(prog * 100)]]; usleep(400000); }
	});
}
- (void)URLSession:(NSURLSession *)s task:(NSURLSessionTask *)t didCompleteWithError:(NSError *)err
{
	if (err) [self fail:@"Internet uzildi. Qayta urinib ko'ring."];
}
@end

static XCSetup *g_xcSetup;
/* AppDelegate dan chaqiriladi: fayllar bo'lsa - darhol o'yin, bo'lmasa - avval yuklash */
static void XC_Start(AppDelegate *app)
{
	void (^launch)(void) = ^{
		[app.gamesController updateGameDirs];
		for (Game *g in app.gamesController.games)
			if ([g.gameDir isEqualToString:@"cstrike"]) { [g startGame]; return; }
	};
	if ([XCSetup ready]) { dispatch_async(dispatch_get_main_queue(), launch); return; }
	g_xcSetup = [[XCSetup alloc] init];
	g_xcSetup.onReady = launch;
	dispatch_async(dispatch_get_main_queue(), ^{ [g_xcSetup start]; });
}
