var ImageFilter = function () {};

ImageFilter.prototype.resize = function (imageUri,win,fail) {
    return PhoneGap.exec(win,fail,'ImageFilter','resize',[imageUri]);
};
ImageFilter.prototype.applyFilter = function (imageUri,filterName,borderName,win,fail) {
    return PhoneGap.exec(win,fail,'ImageFilter','filter',[imageUri,filterName,borderName]);
};
ImageFilter.prototype.rotate = function (imageUri,direction,win,fail) {

    return PhoneGap.exec(win,fail,'ImageFilter','rotate',[imageUri,direction]);
};
ImageFilter.prototype.save = function (imageUri,win,fail) {

    return PhoneGap.exec(win,fail,'ImageFilter','save',[imageUri]);
};
ImageFilter.prototype.install = function(){
    };

if(!window.plugins) {
    window.plugins = {};
}
if (!window.plugins.ImageFilter) {
    window.plugins.ImageFilter= new ImageFilter();
}