// noinspection JSUnresolvedReference

let ig_user_id;
let pageId;
let cPictures = [];

function login() {
    FB.login(function (loginResp) {
        if (loginResp.authResponse) {
            console.log("Welcome");
            FB.api('me/accounts', 'GET', {}, function (respMeAcc) {
                pageId = respMeAcc.data.find(isFSV).id;
                FB.api('/' + pageId, 'GET',  {fields: 'instagram_business_account'}, function (respPageId) {
                    ig_user_id = respPageId.instagram_business_account.id;
                });
            });
        } else {
            console.log("Login failed");
        }
    });
}

function getMedia() {
    FB.api('/' + ig_user_id + '/media', 'GET', {}, function (mediaResp) {
        console.log(mediaResp);
    });
}

function postMedia() {
    FB.api('/'+ ig_user_id + '/media', 'POST', {image_url: 'https://www.chip.de/ii/4/7/2/8/5/5/4/f8c3bf084e08658b.jpg'}, function (container) {
        FB.api('/' + ig_user_id + '/media_publish', 'POST', {creation_id : container.id}, function (uploadID) {
            console.log("Success with id: " + uploadID);
        })
    })
}

function createElementContainer(data){
    let files = data.filelist;
    for (let i = 0; i <= files.length - 1; i++){
        FB.api('/' + ig_user_id + '/media', 'POST', {image_url: 'https://www.chip.de/ii/4/7/2/8/5/5/4/f8c3bf084e08658b.jpg'}, function (container) {
            cPictures.push(container.id);
            if (i === (files.length -1)){
                postCarousel(data);
            }
        });
    }
}
function postCarousel(data) {
    FB.api('/'+ ig_user_id + '/media', 'POST', {media_type: 'CAROUSEL', caption: data.caption, children: cPictures}, function (carouselId) {
        FB.api('/' + ig_user_id + '/media_publish', 'POST', {creation_id : carouselId.id}, function () {
            console.log("Success");
            cPictures = [];
        })
    });
}
function isFSV(itemName) {
    return itemName.name = "TestSite";
}